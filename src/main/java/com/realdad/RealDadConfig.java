package com.realdad;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(RealDadConfig.GROUP)
public interface RealDadConfig extends Config
{
	String GROUP = "realdad";

	@ConfigItem(
		keyName = "cousinName",
		name = "Cousin's name",
		description = "The cousin Dad constantly compares you to",
		position = 0
	)
	default String cousinName()
	{
		return "Kevin";
	}

	@ConfigItem(
		keyName = "showOverlay",
		name = "Show approval overlay",
		description = "Shows Dad's current approval rating of you as a person",
		position = 1
	)
	default boolean showOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "overheadReplies",
		name = "Overhead replies",
		description = "Your character mumbles a reply over their head when Dad speaks (only you can see it)",
		position = 2
	)
	default boolean overheadReplies()
	{
		return true;
	}

	@ConfigItem(
		keyName = "idleNagMinutes",
		name = "AFK nag after",
		description = "Dad notices you've been staring at the screen doing nothing. 0 to disable.",
		position = 3
	)
	@Units(Units.MINUTES)
	@Range(min = 0, max = 60)
	default int idleNagMinutes()
	{
		return 4;
	}

	@ConfigItem(
		keyName = "lateNightRoasts",
		name = "Bedtime enforcement",
		description = "Dad has opinions about you playing between midnight and 5am",
		position = 4
	)
	default boolean lateNightRoasts()
	{
		return true;
	}

	@ConfigItem(
		keyName = "chatterChance",
		name = "Chatter chance",
		description = "Percent chance Dad comments on things he doesn't strictly need to comment on",
		position = 5
	)
	@Units(Units.PERCENT)
	@Range(max = 100)
	default int chatterChance()
	{
		return 35;
	}

	@ConfigItem(
		keyName = "dadColor",
		name = "Dad's chat colour",
		description = "Colour of Dad's messages in the chatbox",
		position = 6
	)
	default Color dadColor()
	{
		return new Color(0x7F3F00);
	}
}
