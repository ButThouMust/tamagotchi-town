# tamagotchi-town
This was the result of about a week or two's worth of work looking at this. I am not particularly interested in working on this anymore (no attachment to or knowledge of the property). Even though I somewhat doubt someone would want to continue work on this, I still want to make these notes and resources available for someone who does, whoever they may be.

# Included items
- A partial disassembly of the game, as a [DiztinGUIsh](https://github.com/IsoFrieze/DiztinGUIsh) project file.
- Two tools coded in Java, for handling the game's graphics compression format. You will need this in order to change the game's font, plus a handful of other graphics in the game that contain Japanese text.
  - A compressor to put your graphics edits into the format the game expects.
  - A decompressor to dump the graphics from the game. Can also use to confirm the compressor "round-trips" from raw data -> compressed data -> identical raw data when decompressed.
- An [Asar](https://github.com/RPGHacker/asar) assembly hack to change the game's text printing routine to use half-width characters (8x16) instead of full-width characters (16x16).
  - See the `images` folder for a sample screenshot.
- A set-up for [Cartographer](https://www.romhacking.net/utilities/647/) to dump out the game's two scripts. One is for descriptions of Tamagotchis. The other is for everything else.
  - I recommend that you regex all the `W16` commands in the output to `W24` commands.
  - An untested sample [Atlas](https://www.romhacking.net/utilities/224/) header for the scripts.
- Table files for the two scripts.
- Assorted "necessary knowledge" notes about the game's inner workings. More detailed notes can be found in the DiztinGUIsh project file.

# Base ROM
First, do not ask me where to obtain a dump of the game. It should be a 1 MB file that conforms to the specification in the [No-Intro database](https://datomatic.no-intro.org/index.php?page=show_record&s=49&n=2979):
```
CRC32:	2c877794
MD5:	c9bfaa0cf1365a1ae1d9be62abe0366b
SHA-1:	e5fa0a457278c831750213014f859e535de06734
SHA-256:	a5fd9970e5d2a23f1f910952823f481572db8bb49f59587b2b901feb66b9b19a
```

# Why I did this
A user `aaamdacagua` in what used to be the romhacking.net Discord (now the Discord for romhack.ing) had made a post/thread there in late October 2023 about wanting to work on this game. They and another user who joined in, `Bunkai`, were having some trouble figuring out the game's text encoding. They had already IDed most of the characters in the font but were stumped about what hex values went to what characters.

TL;DR: The encoding order is conceptually similar to values going `00 01 02 03...`, but not this order verbatim. Importantly, the order made it so a basic relative search wouldn't help much.

I had been working on translating [Otogirisou](https://github.com/ButThouMust/otogirisou-en-beta), saw the post, and thought, "It's a Tamagotchi game. How difficult can it be?" I was itching for a programming/RE challenge anyway after doing translation work.

It took a few hours, but I ended up finding the game's text printing routine, the encoding order, and the script's uncompressed text. I shared my notes there. In retrospect, I do feel like I had robbed the learning opportunity from aaamdacagua, but I suppose that someone would have figured it out eventually.

About a week later, I tried looking into how the game's two fonts were stored in the game. It turned out that they were compressed, and I was able to get a graphics decompressor working for it. It also ended up being the same compression format for a lot of other graphics in the game. I shared the decompressor source code on the Discord, and that was where activity in the Discord post/thread ended.

Fast forward to early October 2024. I remembered I had all this (minus the graphics recompressor; I adapted the one I made for another ROM hacking project) and decided to create this repository just to put it out there and not hoard the info to myself.
