FROM postgres:15.12-alpine3.20

RUN apk add --no-cache ca-certificates rclone
