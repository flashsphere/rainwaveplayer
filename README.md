# Player for Rainwave

Android client for [Rainwave](https://rainwave.cc). Rainwave is an interactive radio website that allows users to request, rate, and vote for songs in real time. The site hosts five separate radio streams and focuses on video game music.

The app provides additional features:
- Automatic song voting
- Android Auto support
- Cast support
- Android/Google TV support

<a href="https://play.google.com/store/apps/details?id=com.flashsphere.rainwaveplayer">
    <img alt="Get it on Google Play" height="80" src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" />
</a>

### Tasker, et al., support

Use Tasker (other similar app) to start playing with a custom sleep timer duration

Send the following intent:
<ul>
  <li>Action: <code>android.intent.action.VIEW</code></li>
  <li>Category: <code>DEFAULT</code></li>
  <li>Data: <code>rw://rainwave.cc/play/{station_id}?sleep_timer={duration}</code>
<br/>
<br/><code>station_id</code>: id of the station (optional)
<br/><code>1</code> - Game, <code>2</code> - OC ReMix, <code>3</code> - Covers, <code>4</code> - Chiptune, <code>5</code> - All, <code>6</code> - Chill
<br/>If no <code>station_id</code> is specified, it will play the last played station.
<br/>
<br/><code>duration</code>: duration of sleep timer (optional), in this format: XhXm, where X is a number
<br/>i.e. <code>1h</code> - 1 hour, <code>1h5m</code> or <code>65m</code> - 65 minutes, <code>15m</code> - 15 minutes
<br/>If no <code>duration</code> is specified, no sleep timer will be configured.
<br/>
<br/>Examples:
  <ol>
    <li><code>rw://rainwave.cc/play/5?sleep_timer=90m</code>
    <br/>This will start playing <code>All</code> station with a 90-minute sleep timer configured
    </li>
    <li><code>rw://rainwave.cc/play/5</code>
    <br/>This will start playing <code>All</code> station without sleep timer configured
    </li>
    <li><code>rw://rainwave.cc/play?sleep_timer=2h</code>
    <br/>This will start playing last played station with a 2-hour sleep timer configured
    </li>
    <li><code>rw://rainwave.cc/play</code>
    <br/>This will start playing last played station without sleep timer configured
    </li>
  </ol>
  </li>
</ul>
