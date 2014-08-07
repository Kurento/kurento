<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<title>kurento-client.js sanity test</title>
<script	src="${kurentoUrl}js/${kurentoLib}.js"></script>
<script>
	window.onload = function() {
		if (typeof ${kurentoObject} == "undefined") {
			document.getElementById("status").value = "Error";
		} else {
			document.getElementById("status").value = "Ok";
		}
	}
</script>
</head>
<body>
	<h1>${kurentoLib}</h1>
	<label for="status">Sanity test</label>
	<input id="status" name="status" style="width: 100px;" />
</body>
</html>
