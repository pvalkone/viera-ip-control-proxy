package com.github.pvalkone.vieraipcontrolproxy

import java.io.{ByteArrayInputStream, IOException, OutputStreamWriter}
import java.net.HttpURLConnection.{HTTP_INTERNAL_ERROR, HTTP_NOT_FOUND, HTTP_NO_CONTENT, HTTP_OK}
import java.net.{HttpURLConnection, InetSocketAddress, URL}
import java.util.concurrent.Executors

import com.github.pvalkone.vieraipcontrolproxy.App.App
import com.github.pvalkone.vieraipcontrolproxy.HdmiCecCommand.HdmiCecCommand
import com.github.pvalkone.vieraipcontrolproxy.RemoteControlKey.RemoteControlKey
import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}

import scala.sys._
import scala.sys.process._
import scala.util.{Failure, Success, Try}

/*
 * A proxy server for allowing IP control of a Panasonic Viera TV over
 * a simple HTTP API.
 *
 * Some European models (e.g. the TX-P55VT60Y) do not support
 * Wake-on-LAN, and thus can't be turned on over Ethernet. This
 * limitation can be worked around by issuing power on/off commands
 * over HDMI-CEC using cec-client from libCEC.
 *
 * Requires Oracle Java 7 and libCEC. Tested on Raspbian Wheezy.
 *
 * On a Raspberry Pi, configure libCEC with the following options:
 *
 * ./configure --with-rpi-include-path=/opt/vc/include \
 * --with-rpi-lib-path=/opt/vc/lib \
 * --enable-rpi
 *
 * To run: $ ./sbt "run <TV_HOSTNAME_OR_IP_ADDRESS>"
 */
object VieraIpControlProxyServer {
  val vieraIpControlPort = 55000

  def main(args: Array[String]): Unit = {
    val (tvHostname, port) = parseCommandLineArguments(args)
    val server = HttpServer.create(new InetSocketAddress(port), 0)
    val tvInetAddress = new InetSocketAddress(tvHostname, vieraIpControlPort)
    server.createContext("/", new VieraIpControlHandler(tvInetAddress))
    server.setExecutor(Executors.newCachedThreadPool())
    server.start()
    println("Listening on port %d, TV at %s:%d".format(port, tvInetAddress.getHostName, tvInetAddress.getPort))
  }

  private def parseCommandLineArguments(args: Array[String]) = {
    val defaultPort = 8080
    if (args.length < 1 || args.length > 2) {
      println(
        """usage: %s tv_hostname [port]
          |  tv_hostname: Hostname or IP address of the Viera TV to connect to
          |  port: Port to bind the proxy server to (default: %d)"""
          .stripMargin.format(getClass.getName, defaultPort))
      exit(1)
    }
    val tvHostname = args(0)
    val port = if (args.length == 2) Integer.parseInt(args(1)) else defaultPort
    (tvHostname, port)
  }
}

class VieraIpControlHandler(tvInetAddress: InetSocketAddress) extends HttpHandler {
  override def handle(exchange: HttpExchange) {
    val status = Try {
      exchange.getRequestURI.getPath match {
        case "/tv/power/on" => sendHdmiCecCommand(HdmiCecCommand.POWER_ON)
        case "/tv/power/off" => sendHdmiCecCommand(HdmiCecCommand.STANDBY)
        case "/tv/key/power" => sendKey(RemoteControlKey.POWER)
        case "/tv/key/menu" => sendKey(RemoteControlKey.MENU)
        case "/tv/key/3d" => sendKey(RemoteControlKey._3D)
        case "/tv/key/tv" => sendKey(RemoteControlKey.TV)
        case "/tv/key/av" => sendKey(RemoteControlKey.AV)
        case "/tv/key/info" => sendKey(RemoteControlKey.INFO)
        case "/tv/key/exit" => sendKey(RemoteControlKey.EXIT)
        case "/tv/key/apps" => sendKey(RemoteControlKey.APPS)
        case "/tv/key/home" => sendKey(RemoteControlKey.HOME)
        case "/tv/key/guide" => sendKey(RemoteControlKey.GUIDE)
        case "/tv/key/up" => sendKey(RemoteControlKey.UP)
        case "/tv/key/right" => sendKey(RemoteControlKey.RIGHT)
        case "/tv/key/down" => sendKey(RemoteControlKey.DOWN)
        case "/tv/key/left" => sendKey(RemoteControlKey.LEFT)
        case "/tv/key/ok" => sendKey(RemoteControlKey.OK)
        case "/tv/key/option" => sendKey(RemoteControlKey.OPTION)
        case "/tv/key/back_return" => sendKey(RemoteControlKey.BACK_RETURN)
        case "/tv/key/red" => sendKey(RemoteControlKey.RED)
        case "/tv/key/green" => sendKey(RemoteControlKey.GREEN)
        case "/tv/key/yellow" => sendKey(RemoteControlKey.YELLOW)
        case "/tv/key/blue" => sendKey(RemoteControlKey.BLUE)
        case "/tv/key/mute" => sendKey(RemoteControlKey.MUTE)
        case "/tv/key/text" => sendKey(RemoteControlKey.TEXT)
        case "/tv/key/sttl" => sendKey(RemoteControlKey.STTL)
        case "/tv/key/aspect" => sendKey(RemoteControlKey.ASPECT)
        case "/tv/key/volume_up" => sendKey(RemoteControlKey.VOLUME_UP)
        case "/tv/key/volume_down" => sendKey(RemoteControlKey.VOLUME_DOWN)
        case "/tv/key/channel_up" => sendKey(RemoteControlKey.CHANNEL_UP)
        case "/tv/key/channel_down" => sendKey(RemoteControlKey.CHANNEL_DOWN)
        case "/tv/key/1" => sendKey(RemoteControlKey.DIGIT_1)
        case "/tv/key/2" => sendKey(RemoteControlKey.DIGIT_2)
        case "/tv/key/3" => sendKey(RemoteControlKey.DIGIT_3)
        case "/tv/key/4" => sendKey(RemoteControlKey.DIGIT_4)
        case "/tv/key/5" => sendKey(RemoteControlKey.DIGIT_5)
        case "/tv/key/6" => sendKey(RemoteControlKey.DIGIT_6)
        case "/tv/key/7" => sendKey(RemoteControlKey.DIGIT_7)
        case "/tv/key/8" => sendKey(RemoteControlKey.DIGIT_8)
        case "/tv/key/9" => sendKey(RemoteControlKey.DIGIT_9)
        case "/tv/key/0" => sendKey(RemoteControlKey.DIGIT_0)
        case "/tv/key/ehelp" => sendKey(RemoteControlKey.EHELP)
        case "/tv/key/last_view" => sendKey(RemoteControlKey.LAST_VIEW)
        case "/tv/key/rewind" => sendKey(RemoteControlKey.REWIND)
        case "/tv/key/play" => sendKey(RemoteControlKey.PLAY)
        case "/tv/key/fast_forward" => sendKey(RemoteControlKey.FAST_FORWARD)
        case "/tv/key/skip_previous" => sendKey(RemoteControlKey.SKIP_PREVIOUS)
        case "/tv/key/pause" => sendKey(RemoteControlKey.PAUSE)
        case "/tv/key/skip_next" => sendKey(RemoteControlKey.SKIP_NEXT)
        case "/tv/key/stop" => sendKey(RemoteControlKey.STOP)
        case "/tv/key/record" => sendKey(RemoteControlKey.RECORD)
        case "/tv/app/netflix" => launchApp(App.NETFLIX)
        case "/tv/app/recorded_tv" => launchApp(App.RECORDED_TV)
        case action => throw new UnsupportedOperationException(s"Unknown action $action")
      }
    } match {
      case Failure(e: UnsupportedOperationException) => HTTP_NOT_FOUND
      case Failure(e) => {
        e.printStackTrace()
        HTTP_INTERNAL_ERROR
      }
      case Success(_) => HTTP_NO_CONTENT
    }
    exchange.sendResponseHeaders(status, -1)
    exchange.close()
  }

  private def sendKey(keyEvent: RemoteControlKey) {
    sendIpControlCommand(SendKey(keyEvent))
  }

  private def launchApp(app: App) {
    sendIpControlCommand(LaunchApp(app))
  }

  private def sendIpControlCommand(command: IpControlCommand) {
    val urn = "panasonic-com:service:p00NetworkControl:1"
    val actionXmlFragment = command match {
      case SendKey(keyEvent) => s"""<u:X_SendKey xmlns:u="urn:$urn">
          <X_KeyEvent>$keyEvent</X_KeyEvent>
        </u:X_SendKey>"""
      case LaunchApp(productId) => s"""<u:X_LaunchApp xmlns:u='urn:$urn'>
          <X_AppType>vc_app</X_AppType>
          <X_LaunchKeyword>product_id=$productId</X_LaunchKeyword>
        </u:X_LaunchApp>"""
    }
    val connection = new URL("http://%s:%d/nrc/control_0".format(tvInetAddress.getHostName, tvInetAddress.getPort)).openConnection().asInstanceOf[HttpURLConnection]
    try {
      connection.setConnectTimeout(5000)
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", "text/xml; charset=UTF-8")
      val action = command match {
        case SendKey(keyEvent) => "X_SendKey"
        case LaunchApp(productId) => "X_LaunchApp"
      }
      connection.setRequestProperty("SOAPACTION", s""""urn:$urn#$action"""")
      connection.setDoOutput(true)
      val writer = new OutputStreamWriter(connection.getOutputStream)
      writer.write(s"""<?xml version="1.0" encoding="UTF-8"?>
        <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
          <s:Body>
            $actionXmlFragment
          </s:Body>
        </s:Envelope>""")
      writer.close()
      val status = connection.getResponseCode
      if (status != HTTP_OK) {
        throw new IOException(s"Unexpected HTTP status $status")
      }
    } finally {
      connection.disconnect()
    }
  }

  private def sendHdmiCecCommand(hdmiCecCommand: HdmiCecCommand) {
    val is = new ByteArrayInputStream(s"$hdmiCecCommand".getBytes("UTF-8"))
    val exitCode = ("/usr/local/bin/cec-client -s -d 1" #< is) ! ProcessLogger(line => ())
    if (exitCode != 0) {
      throw new IOException(s"cec-client returned abnormal exit code $exitCode")
    }
  }

  sealed abstract class IpControlCommand

  case class SendKey(key: RemoteControlKey) extends IpControlCommand

  case class LaunchApp(productId: App) extends IpControlCommand
}

object HdmiCecCommand extends Enumeration {
  type HdmiCecCommand = Value

  val POWER_ON = Value("on 0")
  val STANDBY = Value("standby 0")
}

// https://github.com/samuelmatis/viera-control/blob/master/codes.txt
object RemoteControlKey extends Enumeration {
  type RemoteControlKey = Value

  val POWER = Value("NRC_POWER-ONOFF")

  val MENU = Value("NRC_MENU-ONOFF")
  val _3D = Value("NRC_3D-ONOFF")
  val TV = Value("NRC_TV-ONOFF")
  val AV = Value("NRC_CHG_INPUT-ONOFF")

  val INFO = Value("NRC_INFO-ONOFF")
  val EXIT = Value("NRC_CANCEL-ONOFF")

  val APPS = Value("NRC_APPS-ONOFF")
  val HOME = Value("NRC_HOME-ONOFF")
  val GUIDE = Value("NRC_EPG-ONOFF")

  val UP = Value("NRC_UP-ONOFF")
  val RIGHT = Value("NRC_RIGHT-ONOFF")
  val DOWN = Value("NRC_DOWN-ONOFF")
  val LEFT = Value("NRC_LEFT-ONOFF")
  val OK = Value("NRC_ENTER-ONOFF")

  val OPTION = Value("NRC_SUBMENU-ONOFF")
  val BACK_RETURN = Value("NRC_RETURN-ONOFF")

  val RED = Value("NRC_RED-ONOFF")
  val GREEN = Value("NRC_GREEN-ONOFF")
  val YELLOW = Value("NRC_YELLOW-ONOFF")
  val BLUE = Value("NRC_BLUE-ONOFF")

  val MUTE = Value("NRC_MUTE-ONOFF")
  val TEXT = Value("NRC_TEXT-ONOFF")
  val STTL = Value("NRC_STTL-ONOFF")
  val ASPECT = Value("NRC_DISP_MODE-ONOFF")

  val VOLUME_UP = Value("NRC_VOLUP-ONOFF")
  val VOLUME_DOWN = Value("NRC_VOLDOWN-ONOFF")
  val CHANNEL_UP = Value("NRC_CH_UP-ONOFF")
  val CHANNEL_DOWN = Value("NRC_CH_DOWN-ONOFF")

  val DIGIT_1 = Value("NRC_D1-ONOFF")
  val DIGIT_2 = Value("NRC_D2-ONOFF")
  val DIGIT_3 = Value("NRC_D3-ONOFF")
  val DIGIT_4 = Value("NRC_D4-ONOFF")
  val DIGIT_5 = Value("NRC_D5-ONOFF")
  val DIGIT_6 = Value("NRC_D6-ONOFF")
  val DIGIT_7 = Value("NRC_D7-ONOFF")
  val DIGIT_8 = Value("NRC_D8-ONOFF")
  val DIGIT_9 = Value("NRC_D9-ONOFF")
  val DIGIT_0 = Value("NRC_D0-ONOFF")
  val EHELP = Value("NRC_GUIDE-ONOFF")
  val LAST_VIEW = Value("NRC_R_TUNE-ONOFF")

  val REWIND = Value("NRC_REW-ONOFF")
  val PLAY = Value("NRC_PLAY-ONOFF")
  val FAST_FORWARD = Value("NRC_FF-ONOFF")
  val SKIP_PREVIOUS = Value("NRC_SKIP_PREV-ONOFF")
  val PAUSE = Value("NRC_PAUSE-ONOFF")
  val SKIP_NEXT = Value("NRC_SKIP_NEXT-ONOFF")
  val STOP = Value("NRC_STOP-ONOFF")
  val RECORD = Value("NRC_REC-ONOFF")
}

object App extends Enumeration {
  type App = Value

  val NETFLIX = Value("0010000200000001")
  val RECORDED_TV = Value("0387878700000013")
}
