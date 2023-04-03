# appsyncdemo
Aws AppSync demo implementation for a real time messaging system

# Getting Started

### Reference Documentation
For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/3.0.5/maven-plugin/reference/html/)
* [Create an OCI image](https://docs.spring.io/spring-boot/docs/3.0.5/maven-plugin/reference/html/#build-image)

# Aws AppSync Configuration Details

Create a new AppSync application Name: RT Messaging System

1. Login to your Amazon Account
2. Go to Aws App Sync Console (in your Aws Console search for AppSync)
3. Create a new API
   1. Select "Create a generic real-time API"
   2. Give the new Api a name 'Real Time Messaging System'
   3. Define a schema, click Edit Schema and paste the Schema bellow (should be there already)
```

type Channel {
	name: String!
	data: AWSJSON!
}

type Mutation {
	publish(name: String!, data: AWSJSON!): Channel
}

type Query {
	getChannel: Channel
}

type Subscription {
	subscribe(name: String!): Channel
		@aws_subscribe(mutations: ["publish"])
}

```

4. You need to configure an API Key to access the AppSync. In the Settings section you can add one. Then you paste the url and the api key under src/main/java/resources/config.properties.
   
5. That is it. In this configuration the subscription is called after each mutation.
