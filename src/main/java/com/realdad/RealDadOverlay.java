package com.realdad;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.time.Duration;
import java.time.Instant;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.ProgressBarComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

class RealDadOverlay extends OverlayPanel
{
	private static final Color BAD = new Color(170, 30, 30);
	private static final Color GOOD = new Color(40, 160, 60);

	private final RealDadPlugin plugin;
	private final RealDadConfig config;

	@Inject
	RealDadOverlay(RealDadPlugin plugin, RealDadConfig config)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showOverlay() || !plugin.isProfileLoaded())
		{
			return null;
		}

		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Dad")
			.color(config.dadColor().brighter())
			.build());

		if (plugin.isOutForMilk())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Status:")
				.right("Getting milk")
				.rightColor(Color.LIGHT_GRAY)
				.build());
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Gone for:")
				.right(formatDuration(Duration.between(plugin.getMilkSince(), Instant.now())))
				.rightColor(Color.LIGHT_GRAY)
				.build());
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Returns:")
				.right("99 / pet / total lvl")
				.rightColor(Color.GRAY)
				.build());
			return super.render(graphics);
		}

		int approval = plugin.getApproval();
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Mood:")
			.right(plugin.mood())
			.build());

		ProgressBarComponent bar = new ProgressBarComponent();
		bar.setMinimum(0);
		bar.setMaximum(RealDadPlugin.MAX_APPROVAL);
		bar.setValue(approval);
		bar.setLabelDisplayMode(ProgressBarComponent.LabelDisplayMode.FULL);
		bar.setForegroundColor(blend(approval / (float) RealDadPlugin.MAX_APPROVAL));
		panelComponent.getChildren().add(bar);

		return super.render(graphics);
	}

	private static Color blend(float t)
	{
		return new Color(
			Math.round(BAD.getRed() + (GOOD.getRed() - BAD.getRed()) * t),
			Math.round(BAD.getGreen() + (GOOD.getGreen() - BAD.getGreen()) * t),
			Math.round(BAD.getBlue() + (GOOD.getBlue() - BAD.getBlue()) * t));
	}

	private static String formatDuration(Duration d)
	{
		long days = d.toDays();
		long hours = d.toHours() % 24;
		long minutes = d.toMinutes() % 60;
		if (days > 0)
		{
			return days + "d " + hours + "h";
		}
		if (hours > 0)
		{
			return hours + "h " + minutes + "m";
		}
		return minutes + "m";
	}
}
