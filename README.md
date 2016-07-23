# top-5-repos

A small REST api to return the top 5 repos (by size) for a given github user.

A call to

```bash
curl -i http://localhost:8080/top-5-repos/hamishdickson | less
```

Will return the JSON response

```bash
HTTP/1.1 200 OK
Server: Cowboy
Connection: keep-alive
Content-Type: application/json; charset=UTF-8
Date: Sat, 23 Jul 2016 17:26:01 UTC
Content-Length: 251
Via: 1.1 vegur

[{"name":"hamishdickson/aws-sdk-java","size":267226},{"name":"hamishdickson/emacs-for-scala","size":118097},{"name":"hamishdickson/scala","size":85243},{"name":"hamishdickson/sympy","size":63945},{"name":"hamishdickson/tooting-strength","size":40906}]
```

## Implementation

This app has been implemented using the typelevel project [http4s](http://http4s.org/), scalaz and argonaut

## Notes

- This only works with public repos
- The gh api has rate limiting in place. Currently this is set to 60 requests per hour so don't hit the api with too many requests or you'll get your ip blocked for up to 60mins.
