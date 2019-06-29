<p align="center"><img src ="https://github.com/norcane/workbench/blob/master/assets/logo.png?raw=true" width="200" /></p>

*Workbench* is small *SBT plugin* making development of [Scala.js](https://scala-js.org) applications much more pleasing experience. All you need to do is to add the plugin to your project.

## Key Features

- Starts local web server on `http://localhost:12345` when you enter the SBT console so you can access your *HTML* pages (or any other files).
- Whenever you update your project files (e.g. change some of the *Scala.js* code), connected web browser will be automatically refreshed to reflect the changes.
- All compile output from SBT console is sent to browser console, so you can see compilation status without leaving the browser window.

## Installation

1. **Add the SBT plugin to `project/plugins.sbt`**
   ```
   addSbtPlugin("com.norcane" % "workbench" % "0.1.0")
   ```

1. **Enable the SBT plugin in `build.sbt`**
   ```
   enablePlugins(WorkbenchPlugin)
   ```

## Usage
Now you can start the SBT console and run the following command:
```
sbt ~fastOptJS
```
Now you can open any HTML file via `http://localhost:12345` where path to the file is relative to the project root. For example if you have your HTML file located at `src/main/resources/index.html`, then it will be available on `http://localhost:12345/target/scala-2.12/classes/index.html`.

From now whenever you change any of your project file (*HTML*, *CSS*, *Scala.js*, etc.), *Workbench* will make sure that your browser will be refreshed to reflect all the changes properly. You can also see all SBT compilation output in the browser console, so there's no need to switch back and forth between browser and SBT console.

## Configuration
*Workbench* doesn't need any additiona configuration to work, which makes it extra easy to just drop it into your project and use it. But if you need to, here is the list of options you can use to change the default behaviour:

### Compression
In case that your network connection is slow, you can enable compression of the transferred data using:
```
workbenchCompression := true
```

## Maintainers & contributors
Below is the list of current project maintainers. Feel free to contact us in case of any troubles.

* Václav Švejcar - [@vaclavsvejcar](https://github.com/vaclavsvejcar)


## Credits
*Workbench* is based on [Li Haoyi's](https://github.com/lihaoyi) original [workbench project](https://github.com/lihaoyi/workbench). Big thanks to him & all the project contributors. Also be sure to check his other awesome projects such as [Scalatags](https://github.com/lihaoyi/scalatags) or [Ammonite](https://github.com/lihaoyi/ammonite).