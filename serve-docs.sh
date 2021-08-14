#!/usr/bin/env bash

docker run -p 80:80 -v $(pwd)/docs:/usr/share/nginx/html nginx