====================
MP4 recording format
====================

While **AAC** and **H.264** are the most common audio and video encodings used in the industry, **MP4** is the *de-facto* standard file format to store them. MP4 acts as a "*container*", as its job is to contain one or more audio or video streams, together with any additional data such as subtitles or chapter markers.

When you configure a WebRTC stream over the network, the actual information being sent or received are the individual audio and video streams, possibly encoded as the mentioned AAC and H.264 codecs. However, if you use Kurento's **RecorderEndpoint**, then you need to choose a container format in order to store these streams into an actual file. Here is when the most important decision must be done, as different container formats have different advantages and drawbacks.



MP4 metadata issues
===================

An MP4 file breaks down into several units of data, called **atoms**. Some of these atoms will contain actual audio samples and video frames; some contain accompanying data such as captions, chapter index, title, poster, etc; and lastly some contain so-called *metadata* which is information *about the video itself*, required by the player or receiving decoder to do its job adequately.

In an MP4 container, every movie contains a video metadata atom or "**moov atom**", describing the timescale, duration, and display characteristics of the movie. It acts as an index of the video data, and the file will not start to play until the player can access this index.

By default, MP4 stores the *moov* atom **at the end of the file**. This is fine for local playback, since the entire file is available for playback right away. However, writing the *moov* atom only at the end is a very bad idea for cases such as progressive download or streaming as it would force the download of the entire file first before it will start playback. Similarly, **live recording** in this format can be problematic if the recorder is abruptly stopped for whatever reason (think an unexpected crash), rendering an **unreadable file**.

The solution for these special cases is to move the *moov* atom to the beginning of the file. This ensures that the required movie information is downloaded (or recorded) first, enabling playback to start right away. The placement of the *moov* atom is specified in various software packages through settings such as "progressive download," "fast start," "streaming mode," or similar options.



MP4 Fast-Start in Kurento
-------------------------

An easy solution to the interrupted recording problem is to move the *moov* atom metadata to the beginning of the file. This means that if the recording crashes, all required metadata will have already been written out to the file, so the file will still be readable (at least to some point). Using this technique is called **MP4 Fast-Start**.

MP4 Fast-Start has been tested for recording in Kurento, however the results were not very satisfying. In case of a crash, the files were effectively playable, however this would come at a great cost in terms of resulting file size. MP4 videos with Fast-Start mode enabled would grow in size much faster than those recorded *without* Fast-Start enabled.

Due to this observation, MP4 container format is just considered not the best choice for live recording with Kurento, with WEBM being the better option. Also the popular **MKV** (Matroska container format) has been made available to use for recording since Kurento Media Server release **6.10**. This format is much more robust than MP4 and provides great results for live recording.
