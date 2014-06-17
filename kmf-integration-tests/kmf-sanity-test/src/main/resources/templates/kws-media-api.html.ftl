<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<title>kws-media-api.js sanity test</title>
<script	src="${kwsUrl}js/${kwsLib}.js"></script>
<script>
	window.onload = function() {
		if (typeof ${kwsObject} == "undefined") {
			document.getElementById("status").value = "Error";
		} else {
			document.getElementById("status").value = "Ok";
		}
	}
</script>
</head>
<body>
	<h1>${kwsLib}</h1>
	<label for="status">Sanity test</label>
	<input id="status" name="status" style="width: 100px;" />
</body>
</html>
