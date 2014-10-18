HaxSync Open Source Release
=======

This is the full source code for the latest version of HaxSync I had in my repository.

Notes
---
HaxSync was written while I was a student at university with no experience actually shipping anything. It got unexpectedly big, reaching #28 on Google Play's "Top Sold" apps, getting over 100k installs. I became very passionate about delivering a quality product to my users and spent way too much time on creating new features and fixing bugs.
Unfortunately, I don't have the time or resources to support HaxSync myself anymore. This does not have to be the end though, as I am releasing the source code in hopes of someone else picking up the torch. The Android community's openness was one of the things I always liked about it and in some ways I'm happy to finally participate in that openness myself.
The code is terribly hacky and poorly documented, but I figured I'd rather release this in its current state than endlessly delay it waiting to find time for cleanups. I promise I'm a better engineer now, so please don't judge me for how terrible parts of the source are.



Dependencies
---
* [Facebook for Android SDK](https://github.com/facebook/facebook-android-sdk)
* [ShowcaseView](http://amlcurran.github.io/ShowcaseView/)
* [ColorpickerPreference](https://github.com/attenzione/android-ColorPickerPreference)

HaxSync does not use a build system, as I didn't have any experience with them when I wrote it. I just built it with Android Studio, but importing the project is a bit of a pain. I'd welcome a migration to a more modern build approach!
