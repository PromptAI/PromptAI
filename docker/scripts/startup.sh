#!/bin/bash

# make sure we are using correct encoding
export JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8"

echo "Preparing folders ..."
mkdir -p /data/logs
mkdir -p /data/mysql
mkdir -p /data/mongo
mkdir -p /data/minimalzp/p8s

rm -f /data/logs/*.log
rm -f /data/logs/*.log.*

echo "Starting services ..."
# delegate to supervisor to get the job done
supervisord  -c /etc/supervisord.conf > /dev/null 2>&1

# check mysql running
mysql -s --user=root --password=changeit -e 'show global status like "uptime"' > /dev/null 2>&1
while [ $? -ne 0 ]; do
  echo "Waiting system ready ..."
  sleep 2;
  mysql -s --user=root --password=changeit -e 'show global status like "uptime"' > /dev/null 2>&1
done

supervisorctl start service:backend

curl -f -LI http://127.0.0.1:8000/api/version > /dev/null 2>&1
while [ $? -ne 0 ]; do
    echo "Waiting server ready ..."
    sleep 2;
    curl -f -LI http://127.0.0.1:8000/api/version > /dev/null 2>&1
done

supervisorctl start service:ui
supervisorctl start service:mica
supervisorctl start service:agent
supervisorctl start service:broker

# if we have DB already
mysql -s --user=root --password=changeit -e 'use a1' > /dev/null 2>&1
if [ $? -eq 0 ]
then
  # already exists
  echo "System already initialized!"

else

  echo "Creating a1 account ..."
  curl -X POST -H "Content-Type: application/json" -d '{"name":"local_account","dbName":"a1","admin":"admin@promptai.local","properties":{}}' "http://localhost:8000/api/accounts/init" > /dev/null 2>&1

  agentId=$AGENT_ID
  agentAk=$AGENT_AK
  echo "Creating a1 Agent $agentId  ..."

  ## let's init default agent 4 account
  mysql -s --user=root --password=changeit kbcoredb << EOF

  INSERT INTO agents (id, dbName, ak, status, properties) VALUES ('$agentId', '-', '$agentAk', 1, '{\"lastConnectIn\":\"0\"}');
EOF

   ## init templates
   mysql -s --user=root --password=changeit a1 << EOF
   use a1
   source /scripts/init.sql
EOF

  sleep 2;

  curl -f -LI http://127.0.0.1:8000/api/version > /dev/null 2>&1
  while [ $? -ne 0 ]; do
    echo "Waiting service ready ..."
    sleep 2;
    curl -f -LI http://127.0.0.1:8000/api/version > /dev/null 2>&1
  done
fi

# start chat
sh /scripts/start-chat.sh

echo "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@"
echo " System is ready! You can access the service as follows:                   "
echo "     User: admin@promptai.local                                             "
echo "     Password: promptai                                                       "
echo "                                                                           "
echo " Any question or suggestion, please reach out to info@promptai.us! Enjoy!!! "
echo "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@"
while true;
do
    sleep 5;
done
