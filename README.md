# tamagotchi-town
This repo is the result of about a week or two reverse engineering a Super Famicom/Super Nintendo game, Tamagotchi Town. I RE'd everything I care to with it and am not particularly interested in working on this anymore (no attachment to or knowledge of the property).

Part of me doubts someone would want to continue work on this (possible English translation patch someday?). This being said, I don't want the info to remain trapped in a Discord server or my computer, so I'm making these resources available for whoever does want to work on this.

# Included items
- A partial disassembly of the game, as a [DiztinGUIsh](https://github.com/IsoFrieze/DiztinGUIsh) project file.
  - A .csv file of all the ASM labels I had found for the disassembly.
- Two tools coded in Java, for handling the game's graphics compression format. You will need them in order to change the game's font, plus a handful of other graphics in the game that contain Japanese text.
  - A compressor to put your graphics edits into the format the game expects.
  - A decompressor to dump the graphics from the game. Can also use to confirm that output from the compressor correctly "round-trips" from raw data -> compressed data -> identical raw data when decompressed.
- A set-up for [Cartographer](https://www.romhacking.net/utilities/647/) to dump out the game's two scripts. One is for descriptions of Tamagotchis. The other is for everything else.
  - Table files for the two scripts.
  - An *untested* sample [Atlas](https://www.romhacking.net/utilities/224/) header for the scripts.
- An [Asar](https://github.com/RPGHacker/asar) assembly hack to change the game's text printing routine to use half-width characters (8x16) instead of full-width characters (16x16).
  - It should let you fit up to twice as many characters per row of text than originally possible.
  - See the `images` folder for sample screenshots comparing the original full-width Japanese text and the modded "half-width" (left half only) Japanese text.
- Assorted notes about the game's inner workings. More detailed notes can be found in the DiztinGUIsh project file.

# Suggestions for edits
- The original game has two separate fonts (graphics IDs 0x13 and 0x3E). I recommend making one font, compressing/inserting it, and making *both* IDs 0x13 and 0x3E point to that single data block.
- The game has quite a bit of unused space (blocks of all `FF` bytes) that you can use for graphics edits and the translated scripts.
- For the script dumps, you should regex all the `W16` commands to `W24` commands. That it uses `W16` is an artifact of ignoring the bank bytes of the 24-bit pointers in the text pointer tables (game is LoROM + FastROM, so banks 80-9F instead of 00-1F). Cartographer wouldn't correctly dump text for me otherwise.

# Base ROM specification
First, do not ask me where to obtain a dump of the game. It should be a 1 MB file that conforms to the specification in the [No-Intro database](https://datomatic.no-intro.org/index.php?page=show_record&s=49&n=2979):
```
CRC32:   2c877794
MD5:     c9bfaa0cf1365a1ae1d9be62abe0366b
SHA-1:   e5fa0a457278c831750213014f859e535de06734
SHA-256: 5fd9970e5d2a23f1f910952823f481572db8bb49f59587b2b901feb66b9b19a
```

# Project history
You're probably asking something to the effect of, "If you don't care about Tamagotchis, why did you bother working on this?" Short answer, I was bored.

A user `aaamdacuaga` in what used to be the romhacking.net Discord (now the Discord for romhack.ing) had made a post/thread there in late October 2023 about wanting to work on this game. They had found the font when looking in an SNES emulator's tile viewer, but were having trouble figuring out the game's text encoding. Another user, `Bunkai`, helped ID the characters and was trying to help explain some of the processes for searching for text.

TL;DR: The encoding order is conceptually similar to values going `00 01 02 03...`, but not this order verbatim. Importantly, the distances between character values (often 2, sometimes 0x12) made it so a basic relative search wouldn't help much.

I had been working on translating [Otogirisou](https://github.com/ButThouMust/otogirisou-en-beta), saw the post, and thought, "It's a Tamagotchi game. How difficult can it be?" I was itching for a programming/RE challenge anyway after doing translation work.

It took a few hours, but I ended up finding the game's text printing routine, the encoding order, and the script's uncompressed text. I shared my notes there. In retrospect, I do feel like I had robbed the learning opportunity from aaamdacuaga, but I suppose that someone would have figured it out eventually.

About a week later, I tried looking into how the game's two fonts were stored in the game. I found that they were compressed, and I was able to get a graphics decompressor working for it. It also ended up being the same compression format for a lot of other graphics in the game. I shared the decompressor source code on the Discord, and that was where activity in the Discord post/thread ended.

Fast forward to early October 2024. I remembered I had all this (minus the graphics recompressor; I adapted the one I made for another ROM hacking project) and decided to create this repository just to put it out there and not hoard the info to myself.
