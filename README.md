# MikaelS-ChatServer
Chat server implementation for programming 3

Author: Matti Mikael Särkiniemi
Student-number: 2582610
Email: msarkini18@student.oulu.fi

# !!Curl commands for testing user administration below!! (additional feature):

(registers testuser1):
1. curl -k -d "@testuser1.json" https://localhost:8001/registration -H "Content-Type: application/json"

(registers testadmin):
2. curl -k -d "@testadmin.json" https://localhost:8001/registration -H "Content-Type: application/json"

(testadmin edits testuser1's info):
3. curl -k -d "@userEditTest.json" -u "testadmin" https://localhost:8001/administration -H "Content-Type: application/json"

(testadmin removes testuser1 from the database):
4. curl -k -d "@userRemovalTest.json" -u "testadmin" https://localhost:8001/administration -H "Content-Type: application/json"

(password is seen in testadmin.json)

# !!Testing channel handling!! (additional feature)

Channel handling is done by POST requests to /chat
To test if it works post the following commands to /chat:
1. !create testchannel (this creates a new channel called testchannel)
2. Post a chat message to this channel by supplying any message in json format with "channel" key and value "testchannel"
(testChannelMsg.json can be used)
3. !view testchannel (views messages posted to testchannel)
4. !channels (returns currently existing channels in a list)