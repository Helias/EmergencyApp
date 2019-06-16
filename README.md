# Android-Face-Detection
Android app for face detection in real-time.

## Requirements
- [Android Studio](https://developer.android.com/studio)
- [Android SDK](https://developer.android.com/studio#downloads)
- [OpenCV Library 3.x](https://sourceforge.net/projects/opencvlibrary/files/)

To link the Android SDK to the project, edit the **local.properties** file and correct the path.
About OpenCV Library copy the libraries from **opencv-3.x.x-android-sdk/OpenCV-android-sdk/sdk/native/libs/** to **app/libs/jniLibs/**.
Afterwards, edit the absolute path of the jniLibs file **app/CMakeLists.txt** (line 30).

Open the project folder with **AndroidStudio**, build, compile and run the app 😉.

## Reference

To make this project I followed these tutorials:
- [Import OpenCV Library in the project](https://sriraghu.com/2017/03/11/opencv-in-android-an-introduction-part-1/comment-page-1/)
- [Fix orientation portrait about CameraView](https://github.com/opencv/opencv/issues/4704)
- AndroidFaceDetection [github](https://github.com/eddydn/AndroidFaceDetection), [youtube video](https://www.youtube.com/watch?v=3XxOuvAngU4)
