package com.example.multitenant.socket;

import com.corundumstudio.socketio.SocketIOServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SocketIoServerConfig {
	private static final Logger log = LoggerFactory.getLogger(SocketIoServerConfig.class);

	@Bean
	public SocketIOServer socketIOServer(@Value("${socketio.port:9092}") int port) {
		com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
		config.setPort(port);
		config.setOrigin("*");
		config.setHostname("0.0.0.0");
		// Socket.IO 4.x 호환을 위한 설정
		config.setUpgradeTimeout(10000);
		config.setPingTimeout(60000);
		config.setPingInterval(25000);
		
		// Authorization listener - 모든 연결 허용
		config.setAuthorizationListener(data -> {
			String origin = data.getHttpHeaders().get("Origin");
			log.debug("[AUTH] Authorizing handshake: origin={}, params={}", origin, data.getUrlParams());
			return com.corundumstudio.socketio.AuthorizationResult.SUCCESSFUL_AUTHORIZATION;
		});

		log.info("=== Socket.IO Server Config ===");
		log.info("Port: {}", port);
		log.info("Origin: *");
		log.info("Hostname: 0.0.0.0");

		SocketIOServer server = new SocketIOServer(config);

		server.addConnectListener(client -> {
			log.info("[CONNECT] Client connected: sessionId={}, remoteAddress={}",
					client.getSessionId(), client.getRemoteAddress());
		});

		server.addDisconnectListener(client -> {
			log.info("[DISCONNECT] Client disconnected: sessionId={}", client.getSessionId());
		});

		server.start();
		log.info("Socket.IO server started on port {}", port);
		return server;
	}

	@Bean
	public DisposableBean socketIoStopper(SocketIOServer server) {
		return server::stop;
	}
}

