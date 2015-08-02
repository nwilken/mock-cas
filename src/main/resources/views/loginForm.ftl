<!DOCTYPE html>
<html>
<head>
	<meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">

	<link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/css/bootstrap.min.css">

	<style>
		body {
			background-color: #ddd
		}
		.login-panel {
			width: 330px;
			margin: 0 auto;
			padding-top: 100px;
		}
		.login-panel button {
			width: 100%;
		}
	</style>
</head>
<body>
	<div class="container">
		<div class="login-panel">
			<h1>Mock CAS</h1>
			<form action="/cas/login?service=${service}" method="POST">
				<div class="form-group">
					<label>Name</label>
					<input type="text" class="form-control" name="userName"/>
				</div>
				<button type="submit" class="btn btn-primary">Login</button>
			</form>
		</div>
	</div>
</body>
</html>