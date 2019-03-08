ActivityThread里有
```
final ArrayList<Application> mAllApplications
            = new ArrayList<Application>();
Application mInitialApplication;
```

这个mInitialApplication是ActivityThread认为的真正的Application

一个loadedApk里有一个Application