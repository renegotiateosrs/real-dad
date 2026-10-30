package com.realdad;

import com.google.inject.Provides;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import javax.inject.Inject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Real Dad",
	description = "Your dad now watches you play. He has notes. He may go out for milk.",
	tags = {"dad", "fun", "meme", "joke", "approval", "milk"}
)
public class RealDadPlugin extends Plugin
{
	static final int MAX_APPROVAL = 100;
	private static final int DEFAULT_APPROVAL = 50;
	private static final int RETURN_APPROVAL = 40;

	private static final String KEY_APPROVAL = "approval";
	private static final String KEY_MILK_SINCE = "milkSince";

	private static final int CLIENT_TICKS_PER_MINUTE = 60 * 50;
	private static final int OVERHEAD_CYCLES = 150;
	private static final Duration CHATTER_COOLDOWN = Duration.ofSeconds(45);
	private static final Duration BANK_COOLDOWN = Duration.ofMinutes(15);
	private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

	private static final long BROKE_GP = 1_000_000L;
	private static final long RICH_GP = 100_000_000L;

	@Inject
	private Client client;

	@Inject
	private RealDadConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private RealDadOverlay overlay;

	@Getter(AccessLevel.PACKAGE)
	private int approval = DEFAULT_APPROVAL;

	/**
	 * When Dad left for milk, or null if he is home (for now).
	 */
	@Getter(AccessLevel.PACKAGE)
	private Instant milkSince;

	@Getter(AccessLevel.PACKAGE)
	private boolean profileLoaded;

	private final Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
	private boolean needsLoginInit = true;
	private Instant sessionStart;
	private int hoursAnnounced;
	private boolean idleNagged;
	private Instant lastChatter = Instant.EPOCH;
	private Instant lastBankRoast = Instant.EPOCH;

	@Provides
	RealDadConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RealDadConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		needsLoginInit = true;
		profileLoaded = false;
		levels.clear();
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		levels.clear();
		profileLoaded = false;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING)
		{
			needsLoginInit = true;
			levels.clear();
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		if (needsLoginInit)
		{
			needsLoginInit = false;
			onLogin();
			return;
		}

		checkIdle();
		checkSessionLength();
	}

	private void onLogin()
	{
		loadProfile();
		sessionStart = Instant.now();
		hoursAnnounced = 0;
		idleNagged = false;

		if (isOutForMilk())
		{
			return;
		}

		LocalTime now = LocalTime.now();
		if (config.lateNightRoasts() && now.getHour() < 5)
		{
			say(fill(DadLines.pick(DadLines.LATE_NIGHT)).replace("{time}", now.format(CLOCK)), -3);
		}
		else
		{
			say(fill(DadLines.pick(DadLines.GREETING)), 0);
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		Skill skill = event.getSkill();
		int level = event.getLevel();
		Integer previous = levels.put(skill, level);

		// First sighting of a skill this session is just the server telling us what we already have
		if (previous == null || level <= previous)
		{
			return;
		}

		int oldTotal = totalLevel() - (level - previous);
		int newTotal = totalLevel();
		boolean crossedTotalMilestone = oldTotal / 100 != newTotal / 100;

		if (level >= 99)
		{
			dadComesHome();
			say(fill(DadLines.pick(DadLines.MAX_LEVEL), skill, level), 40, true);
			return;
		}

		if (crossedTotalMilestone && isOutForMilk())
		{
			dadComesHome();
			say("Total level " + newTotal + "? Thought I'd come back and see for myself.", 0, true);
			return;
		}

		if (isOutForMilk())
		{
			maybePostcard();
			return;
		}

		boolean milestone = level % 10 == 0;
		if (milestone || rollChatter())
		{
			say(fill(DadLines.pick(DadLines.LEVEL_UP), skill, level), milestone ? 3 : 1);
		}
		else
		{
			adjustApproval(1);
		}
	}

	@Subscribe
	public void onActorDeath(ActorDeath event)
	{
		if (event.getActor() != client.getLocalPlayer() || isOutForMilk())
		{
			return;
		}

		say(fill(DadLines.pick(DadLines.DEATH)), -8);
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		ChatMessageType type = event.getType();
		String message = Text.removeTags(event.getMessage());

		if (type == ChatMessageType.GAMEMESSAGE || type == ChatMessageType.SPAM)
		{
			if (message.startsWith("You have a funny feeling like you")
				|| message.startsWith("You feel something weird sneaking into your backpack"))
			{
				dadComesHome();
				say(fill(DadLines.pick(DadLines.PET)), MAX_APPROVAL, true);
			}
			else if (isOutForMilk())
			{
				return;
			}
			else if (message.startsWith("New item added to your collection log"))
			{
				say(fill(DadLines.pick(DadLines.COLLECTION_LOG)), 2);
			}
			else if (message.startsWith("Valuable drop:") && rollChatter())
			{
				say(fill(DadLines.pick(DadLines.VALUABLE_DROP)), 1);
			}
			return;
		}

		if (type == ChatMessageType.PUBLICCHAT && !isOutForMilk())
		{
			Player local = client.getLocalPlayer();
			if (local == null || local.getName() == null)
			{
				return;
			}

			if (Text.sanitize(event.getName()).equals(Text.sanitize(local.getName())) && rollChatter())
			{
				say(fill(DadLines.pick(DadLines.PUBLIC_CHAT)), 0);
			}
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() != InventoryID.BANK || isOutForMilk())
		{
			return;
		}

		Instant now = Instant.now();
		if (Duration.between(lastBankRoast, now).compareTo(BANK_COOLDOWN) < 0)
		{
			return;
		}
		lastBankRoast = now;

		ItemContainer bank = event.getItemContainer();
		long gp = (long) bank.count(ItemID.COINS) + 1000L * bank.count(ItemID.PLATINUM);
		String formatted = String.format(Locale.ENGLISH, "%,d", gp);

		if (gp < BROKE_GP)
		{
			say(fill(DadLines.pick(DadLines.BANK_BROKE)).replace("{gp}", formatted), -1);
		}
		else if (gp >= RICH_GP)
		{
			say(fill(DadLines.pick(DadLines.BANK_RICH)).replace("{gp}", formatted), 2);
		}
	}

	private void checkIdle()
	{
		int nagMinutes = config.idleNagMinutes();
		if (nagMinutes <= 0 || isOutForMilk())
		{
			return;
		}

		int idleTicks = Math.min(client.getMouseIdleTicks(), client.getKeyboardIdleTicks());
		if (idleTicks < nagMinutes * CLIENT_TICKS_PER_MINUTE)
		{
			idleNagged = false;
			return;
		}

		if (!idleNagged)
		{
			idleNagged = true;
			int minutes = idleTicks / CLIENT_TICKS_PER_MINUTE;
			String time = minutes + (minutes == 1 ? " minute" : " minutes");
			say(fill(DadLines.pick(DadLines.AFK)).replace("{time}", time), -2);
		}
	}

	private void checkSessionLength()
	{
		if (sessionStart == null)
		{
			return;
		}

		int hours = (int) Duration.between(sessionStart, Instant.now()).toHours();
		if (hours <= hoursAnnounced)
		{
			return;
		}
		hoursAnnounced = hours;

		if (isOutForMilk())
		{
			maybePostcard();
			return;
		}

		LocalTime now = LocalTime.now();
		if (config.lateNightRoasts() && now.getHour() < 5)
		{
			say(fill(DadLines.pick(DadLines.LATE_NIGHT)).replace("{time}", now.format(CLOCK)), -3);
			return;
		}

		say(fill(DadLines.pick(DadLines.HOURLY)).replace("{hours}", Integer.toString(hours)), hours >= 3 ? -2 : 0);
	}

	private void maybePostcard()
	{
		if (ThreadLocalRandom.current().nextInt(100) < 20)
		{
			sendDadMessage(fill(DadLines.pick(DadLines.POSTCARD)), 0);
		}
	}

	private boolean rollChatter()
	{
		Instant now = Instant.now();
		if (Duration.between(lastChatter, now).compareTo(CHATTER_COOLDOWN) < 0)
		{
			return false;
		}

		if (ThreadLocalRandom.current().nextInt(100) >= config.chatterChance())
		{
			return false;
		}

		lastChatter = now;
		return true;
	}

	private void say(String line, int delta)
	{
		say(line, delta, delta > 5);
	}

	/**
	 * Dad speaks, approval moves, and if it bottoms out he goes for milk.
	 */
	private void say(String line, int delta, boolean happy)
	{
		int applied = adjustApproval(delta);
		sendDadMessage(line, applied);
		reply(happy);

		if (approval <= 0 && !isOutForMilk())
		{
			sendDadMessage(DadLines.pick(DadLines.LEAVING), 0);
			milkSince = Instant.now();
			saveProfile();
		}
	}

	private int adjustApproval(int delta)
	{
		int before = approval;
		approval = Math.max(0, Math.min(MAX_APPROVAL, approval + delta));
		if (approval != before)
		{
			saveProfile();
		}
		return approval - before;
	}

	private void dadComesHome()
	{
		if (!isOutForMilk())
		{
			return;
		}

		milkSince = null;
		approval = RETURN_APPROVAL;
		saveProfile();
		sendDadMessage(DadLines.pick(DadLines.RETURNING), 0);
	}

	private void sendDadMessage(String line, int delta)
	{
		ChatMessageBuilder builder = new ChatMessageBuilder()
			.append(config.dadColor(), "Dad: ")
			.append(ChatColorType.NORMAL)
			.append(line);

		if (delta != 0)
		{
			builder.append(ChatColorType.HIGHLIGHT)
				.append(String.format(" (Approval %+d)", delta));
		}

		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.GAMEMESSAGE)
			.runeLiteFormattedMessage(builder.build())
			.build());
	}

	private void reply(boolean happy)
	{
		Player local = client.getLocalPlayer();
		if (!config.overheadReplies() || local == null)
		{
			return;
		}

		local.setOverheadText(DadLines.pick(happy ? DadLines.REPLIES_HAPPY : DadLines.REPLIES));
		local.setOverheadCycle(OVERHEAD_CYCLES);
	}

	private String fill(String line)
	{
		return line.replace("{cousin}", config.cousinName());
	}

	private String fill(String line, Skill skill, int level)
	{
		return fill(line)
			.replace("{skill}", skill.getName())
			.replace("{level}", Integer.toString(level));
	}

	private int totalLevel()
	{
		int total = 0;
		for (int level : levels.values())
		{
			total += level;
		}
		return total;
	}

	boolean isOutForMilk()
	{
		return milkSince != null;
	}

	String mood()
	{
		if (isOutForMilk())
		{
			return "Getting milk";
		}
		if (approval <= 15)
		{
			return "Googling adoption law";
		}
		if (approval <= 35)
		{
			return "Sighing loudly";
		}
		if (approval <= 60)
		{
			return "Tolerating you";
		}
		if (approval <= 85)
		{
			return "Almost proud";
		}
		return "Brags at the pub";
	}

	private void loadProfile()
	{
		Integer storedApproval = configManager.getRSProfileConfiguration(RealDadConfig.GROUP, KEY_APPROVAL, Integer.class);
		Long storedMilk = configManager.getRSProfileConfiguration(RealDadConfig.GROUP, KEY_MILK_SINCE, Long.class);

		approval = storedApproval == null ? DEFAULT_APPROVAL : storedApproval;
		milkSince = storedMilk == null || storedMilk <= 0 ? null : Instant.ofEpochMilli(storedMilk);
		profileLoaded = true;
	}

	private void saveProfile()
	{
		if (!profileLoaded)
		{
			return;
		}

		configManager.setRSProfileConfiguration(RealDadConfig.GROUP, KEY_APPROVAL, approval);
		if (milkSince == null)
		{
			configManager.unsetRSProfileConfiguration(RealDadConfig.GROUP, KEY_MILK_SINCE);
		}
		else
		{
			configManager.setRSProfileConfiguration(RealDadConfig.GROUP, KEY_MILK_SINCE, milkSince.toEpochMilli());
		}
	}
}
