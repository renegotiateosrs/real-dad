package com.realdad;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Everything Dad has ever said to you, organised by trauma.
 * Placeholders: {cousin} {skill} {level} {hours} {gp} {time}
 */
final class DadLines
{
	private DadLines()
	{
	}

	static final String[] LEVEL_UP = {
		"{level} {skill}. {cousin} got that the week he was born.",
		"Level {level}? Neat. Does it pay?",
		"Oh, {skill} went up. Your mother and I will tell the neighbours. Quietly.",
		"{level} {skill}. I'm not mad. I'm just... yeah.",
		"Congrats on {level} {skill}. Now do your taxes with the same energy.",
		"You know what else levels up? Interest. On the money you owe me.",
		"{cousin} skipped {skill} entirely. Went straight to having a pension.",
		"Good. Great. {level}. Put it on the fridge next to nothing else.",
		"When I was your age I got {level} {skill} uphill both ways. In the snow. On mobile.",
		"I see you're levelling {skill} instead of returning my calls.",
	};

	static final String[] MAX_LEVEL = {
		"99 {skill}. ...Kiddo, I— I have something in my eye. It's the cape. It's very bright.",
		"Ninety-nine. I'm putting this one on the fridge. I'm buying a second fridge.",
		"99 {skill}. Called {cousin}'s dad. He hung up. Best day of my life.",
		"You maxed {skill}? I take back about a third of what I said.",
	};

	static final String[] DEATH = {
		"You died. Again. Your grandfather died ONCE and he made it count.",
		"I didn't raise you to lie face down on the floor. That's what your uncle does.",
		"Dead. Cool. I'll get the spade.",
		"Did you just die to that? I saw that. The whole street saw that.",
		"Respawning isn't a personality, champ.",
		"Your mother asked how you're doing. I told her 'Lumbridge'.",
		"{cousin} has never died. He has a 401k.",
	};

	static final String[] PET = {
		"A PET?! That's my kid! I always believed in you. I never said otherwise. Delete the chat logs.",
		"You got a pet before {cousin} got a pet. I am calling EVERYONE.",
		"Little guy follows you around? Huh. Someone finally looks up to you.",
	};

	static final String[] COLLECTION_LOG = {
		"Collecting things again. Your mother collected things. Then she collected half the house.",
		"Another one for the log. Is the log employed?",
		"Nice. Put that in the collection with the rest of my disappointment.",
	};

	static final String[] VALUABLE_DROP = {
		"A valuable drop! Is that rent? Tell me that's rent.",
		"Oh good, money. You can start paying for the wifi.",
		"Don't spend it all on bonds. ...You're going to spend it all on bonds.",
	};

	static final String[] AFK = {
		"Are you playing or is the game playing you?",
		"You've been staring at that thing for {time}. Blink. Blink for me.",
		"Hello? HELLO? I can see the screen. Nothing is happening.",
		"Is this one of the 'AFK skills' your friends do? Is that also your career plan?",
		"Even the game's gone quiet. It's embarrassed for you.",
	};

	static final String[] HOURLY = {
		"You've been on this for {hours} hour(s). The lawn has been on for {hours} week(s).",
		"{hours} hour(s). I could've driven to the coast. I could've NOT come back.",
		"{hours} hour(s) straight. Do you need a drink? Water. I meant water.",
		"Hour {hours}. Your posture has a posture problem.",
		"{hours} hour(s). Is the chair part of you now? Should we get it a name?",
	};

	static final String[] LATE_NIGHT = {
		"It is {time}. In the MORNING. Some of us have work.",
		"{time}?! I heard the clicking through two walls.",
		"Bed. Now. I don't care if it's 'a good spawn'.",
	};

	static final String[] BANK_BROKE = {
		"{gp} gp in the bank. I've found more than that in the sofa.",
		"That's your whole bank? {gp}? I was paying a mortgage at your age.",
		"{gp} gp. The bank is charging YOU to look at it.",
	};

	static final String[] BANK_RICH = {
		"{gp} gp?! And you still borrowed twenty quid off me in March.",
		"Look at all that gold. Your mother and I are 'between cars', by the way.",
		"{gp}. Huh. Remember who bought you your first membership.",
	};

	static final String[] PUBLIC_CHAT = {
		"Who are you talking to? Do they have a job?",
		"Don't type to strangers. Type to your father. Who is right here.",
		"Did you just say that in public? With my surname?",
		"Speak up, the whole server can't hear you.",
	};

	static final String[] LEAVING = {
		"Right. I'm going out for milk.",
		"I'm popping out for milk. Don't wait up. Don't wait at all.",
		"That's it. Milk. I'm getting milk. Goodbye.",
	};

	static final String[] POSTCARD = {
		"[Postcard] Greetings from Karamja. Still getting milk.",
		"[Postcard] The milk queue at the Grand Exchange is very long. Years, probably.",
		"[Postcard] Met a nice woman in Varrock. She has a cow. Thinking things over.",
		"[Postcard] Found milk. Lost interest. Will write again at your next 99.",
		"[Postcard] Saw {cousin}. He's doing great. Just thought you should know.",
	};

	static final String[] RETURNING = {
		"Hey kiddo. I'm back. They were out of 2%.",
		"So, I got the milk. Took a while. Saw you did something impressive. Unrelated.",
		"I'm back. The milk is off now. Like my opinion of you was. Was! Past tense!",
	};

	static final String[] GREETING = {
		"Oh. You're on again.",
		"Back on the computer, I see.",
		"Logged in, have we? Did you take the bins out first?",
		"Hello kiddo. {cousin} says hi. He's a dentist now.",
	};

	static final String[] REPLIES = {
		"Sorry dad",
		"ok dad",
		"*sigh*",
		"I know dad",
		"yes dad",
		"...",
		"not now dad",
		"dad please",
	};

	static final String[] REPLIES_HAPPY = {
		"thanks dad!!",
		"love you dad",
		"wait really?",
		":')",
	};

	static String pick(String[] lines)
	{
		return lines[ThreadLocalRandom.current().nextInt(lines.length)];
	}
}
