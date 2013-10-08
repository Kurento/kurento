window.addEventListener('load', function()
{
  var kmfMedia = new KwsMedia();

  kmfMedia.onopen = function(event)
  {
    var pipeline = this.createMediaPipeline();

    var uriSource       = pipeline.createMediaElement('UriSource');
    var jackVaderFilter = pipeline.createMediaElement('JackVaderFilter');
    var uriEndPoint     = pipeline.createMediaElement('UriEndPoint');

    uriSource.uri = "http://localhost:8000/video.avi";

    pipeline.connect(uriSource, jackVaderFilter, uriEndPoint);

    document.getElementById("videoOutput").src = uriEndPoint.getUri();

    uriEndPoint.start();
  };
  kmfMedia.onerror = function(event)
  {
    console.error(event);
  }
});
