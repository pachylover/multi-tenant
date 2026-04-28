package com.example.multitenant.socket;

import com.corundumstudio.socketio.SocketIOServer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SocketIoServerConfig {
	@Bean
	public SocketIOServer socketIOServer(@Value("${socketio.port:9092}") int port) {
		com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
		config.setPort(port);
		config.setOrigin("*");

		SocketIOServer server = new SocketIOServer(config);
		server.start();
		return server;
	}

	@Bean
	public DisposableBean socketIoStopper(SocketIOServer server) {
		return server::stop;
	}
}

