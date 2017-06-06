FROM kurento/node-chrome-dev-debug:2.47.1

RUN apt-get update && apt-get install -y iproute

COPY entry_point_dnat.sh /opt/bin/entry_point_dnat.sh
RUN chmod +x /opt/bin/entry_point_dnat.sh

ENTRYPOINT /opt/bin/entry_point_dnat.sh
