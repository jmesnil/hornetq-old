$(document).ready(function(){

   var topic_url = "topics/jms.topic.trackers";
   var publisher;
   var map, marker;
   var position;

   var alias = prompt("Enter your unique alias:");
   init_publisher();
   var watcher = init_geolocation_watcher();

   function init_publisher() {
      $.get(topic_url, function(data, status, xhr) {
         publisher = xhr.getResponseHeader("msg-create");
         if (position) {
            broadcast(alias, position);
         }
      });
   }

   function init_geolocation_watcher() {
      return navigator.geolocation.watchPosition(
         // success
         function(pos) {
            position = pos
            show(alias, position);
            broadcast(alias, position);
         },
         // error
         function() {
            $("#geo-wrapper").html('Unable to determine your location.');
         });
   }

   function broadcast(alias, position)
   {
      if (publisher) {
         $.ajax({
            type: 'POST',
            url: publisher,
            data: JSON.stringify({
               alias: alias,
               position: position
            }),
            contentType: "application/json",
            success:  function(data, status, xhr) {
               publisher = xhr.getResponseHeader("msg-create-next");
               debug(">");
            }
         });
      }
   }

   function create_map(position) {
      map = new google.maps.Map($("#geo-wrapper").get(0), {
         zoom: 14,
         center: position,
         navigationControl: true,
         mapTypeControl: true,
         scaleControl: true,
         mapTypeId: google.maps.MapTypeId.ROADMAP});
   }

   function show(who, loc) {
      var position = new google.maps.LatLng(loc.coords.latitude, loc.coords.longitude);
      if (!map) {
         create_map(position);
      }
      if (!marker) {
         marker = new google.maps.Marker({
                  position: position,
                  map: map,
                  title: "I am here"});
      } else {
         marker.setPosition(position);
      }
      debug(".");
   }

   $(window).unload(function() {
      broadcast(alias, null);
      if (watcher) {
         navigator.geolocation.clearWatch(watcher);
      }
   });

   function debug(str) {
     $("#debug").append(str);
   };
});