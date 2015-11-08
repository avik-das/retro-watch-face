Retro Android Wear Watch Face
=============================

![](https://raw.githubusercontent.com/avik-das/retro-watch-face/master/img/readme-preview.png)

A retro-themed watch face for Android wear devices. I made this to learn about developing watch faces for Android wear, as well as learning the technologies available on such devices. I've also been using this as my primary watch face.

Hopefully, this codebase will serve as a fairly minimal example of the various pieces needed to develop your own watch face. Finding good examples was something I struggled with when learning this material, though [the official documentation](http://developer.android.com/training/wearables/watch-faces/index.html) was quite helpful.

Getting the watch face
----------------------

Due to the use of copyrighted images, I have not made this available on the Play Store. If you're familiar with building Android applications, you can clone this repository and build it yourself.

Features
--------

- Randomly switches between a set of available retro game-inspired images. Currently included: Super Mario Bros., The Legend of Zelda, Megaman and Pokemon Gold/Silver.

- Automatically goes into a darker "night-mode" between 6PM and 6AM.

- An included app for your phone to turn "night-mode" on or off.

Device compatibility
--------------------

Because this was a personal project, I've only tested this on my own device, the LG G Watch (the original square one). I've tried to write the code in such a way as to dynamically scale to the correct screen resolution, but I haven't tested it on any other device. Pull requests are welcome on this front!
