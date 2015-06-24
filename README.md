# excursion-demos
This branch of the project integrates several other efforts into a single application.  As such this is code should
be considered derivative and not original work.  Having drawn from several sources I am unable to specify directly
the original source but can indicate the following projects as source:
- [scalajs-spa-tutorial](https://github.com/ochrons/scalajs-spa-tutorial)
- [akka-http-scala-js-websocket-chat](https://github.com/jrudolph/akka-http-scala-js-websocket-chat)
- [scalajs-react](https://github.com/japgolly/scalajs-react)

### The primary focus of this branch is on using the following technologies to create a reactive SPA (Single Page Application)
-  [Scala](http://www.scala-lang.org)
-  [ScalaJS](http://www.scala-js.org)
-  [ReactJS via ScalaJS](https://github.com/japgolly/scalajs-react)
-  [Akka HTTP (in lieu of Spray)](http://doc.akka.io/docs/akka-stream-and-http-experimental/1.0-M2/scala/http/)
-  [SBT](http://www.scala-sbt.org)

### The remaining following technical features are enabled:
- [x] Automatic server restart via SBT plugin *sbt-revolver*
- [x] ScalaJS/ScalaVM cross project compilation via SBT plugin *sbt-scalajs*
- [x] Display of project dependencies via SBT plugin *sbt-dependency-graph*
- [x] Auto refresh of HTML containing ScalaJS via SBT plugin *workbench*
- [ ] Deployment to Docker via SBT plugin *sbt-native-packager*
- [x] Client-side to Server-side logging via AjaxAppender *thanks to [scalajs-spa-tutorial](https://github.com/ochrons/scalajs-spa-tutorial)*
- [x] ScalaJS-to-Scala RPC over Ajax *thanks to [autowire](https://github.com/lihaoyi/autowire)*
- [x] Fast Binary object serialization over WebSocket *thanks to [boopickle](https://github.com/ochrons/boopickle) and [Akka HTTP](http://doc.akka.io/docs/akka-stream-and-http-experimental/1.0-M2/scala/http/)*
- [x] Fast Template System across project Scala/ScalaJS *thanks to [scalatags](https://github.com/lihaoyi/scalatags)*


