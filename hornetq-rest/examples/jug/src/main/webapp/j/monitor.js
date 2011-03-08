$(document).ready(function(){

   var client, sub_id;
   var map, trackers = {};

   function create_map(center)
   {
      map = new google.maps.Map($("#geo-wrapper").get(0), {
         zoom: 14,
         center: center,
         navigationControl: true,
         mapTypeControl: true,
         scaleControl: true,
         mapTypeId: google.maps.MapTypeId.ROADMAP});
   }

   function show(who, loc) {
      if (!loc) {
         trackers[who].setMap(null);
         trackers[who] = null;
         debug(who + " has left");
      }
      var position = new google.maps.LatLng(loc.coords.latitude, loc.coords.longitude);
      if (!map) {
         create_map(position);
      }
      if (!trackers[who]) {
         trackers[who] = new google.maps.Marker({
            position: position,
            map: map,
            title: who + " is here"});
      } else {
         trackers[who].setPosition(position);
      }
      debug(who + " has moved!");
   }

   $('#connect_form').submit(function() {
      var url = "ws://localhost:61614/stomp";
      var login = "guest";
      var passcode = "guest";
      var destination = "jms.topic.trackers";

      client = Stomp.client(url);

      var onconnect = function(frame) {
         debug("connected to Stomp");
         $('#connect').fadeOut({ duration: 'fast' });
         $('#disconnect').fadeIn();
         $('#geo-wrapper').fadeIn();

         sub_id = client.subscribe(destination, function(message) {
            payload = JSON.parse(message.body);
            show(payload.alias, payload.position);
         });
      };
      client.connect(login, passcode, onconnect);

      return false;
   });

   $('#disconnect_form').submit(function() {
      client.unsubscribe(sub_id);
      client.disconnect();

      $('#disconnect').fadeOut({ duration: 'fast' });
      $('#geo-wrapper').fadeOut({ duration: 'fast' });
      $('#connect').fadeIn();

      return false;
   });

   // this allows to display debug logs directly on the web page
   function debug(str) {
      $("#debug").append(str + "\n");
   };
});