package com.zx.sms.connect.manager.cmpp;

import com.zx.sms.connect.manager.CMPPEndpointManager;
import com.zx.sms.handler.api.BusinessHandlerInterface;
import com.zx.sms.handler.api.gate.SessionConnectedHandler;
import com.zx.sms.handler.api.smsbiz.MessageReceiveHandler;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 *经测试，35个连接，每个连接每200/s条消息
 *lenovoX250能承担7000/s消息编码解析无压力。
 *10000/s的消息服务不稳定，开个网页，或者打开其它程序导致系统抖动，会有大量消息延迟 (超过500ms)
 *
 *低负载时消息编码解码可控制在10ms以内。
 *
 */


public class TestCMPPEndPoint {
	private static final Logger logger = LoggerFactory.getLogger(TestCMPPEndPoint.class);

	@Test
	public void testCMPPEndpoint() throws Exception {

		final CMPPEndpointManager manager = CMPPEndpointManager.INS;

		// 注意下面所有Entity的Id字段是不允许重复的，Id标识整个JVM唯一的一个网关端口
		// 创建一个CMPP的服务端，模拟一个短信网关
		CMPPServerEndpointEntity server = new CMPPServerEndpointEntity();
		server.setId("server");
		server.setHost("127.0.0.1");
		server.setPort(7891);
		server.setValid(true);
		server.setUseSSL(false);  //不使用SSL加密流量

		//给这个网关增加一个允许接入的账号
		CMPPServerChildEndpointEntity child = new CMPPServerChildEndpointEntity();
		child.setId("child");
		child.setChartset(Charset.forName("utf-8"));
		child.setGroupName("test");
		child.setUserName("901782");
		child.setPassword("ICP");
		child.setValid(true);
		child.setWindows((short)16);
		child.setVersion((short)48);
		child.setMaxChannels((short)20);
		child.setRetryWaitTimeSec((short)100);
		child.setMaxRetryCnt((short)3);

		//给这个账号添加一个业务处理: SessionConnectedHandler 。当client连接完成,用户名密码正确后，立即给Client发送200000条短信
		List<BusinessHandlerInterface> serverhandlers = new ArrayList<BusinessHandlerInterface>();
		serverhandlers.add(new SessionConnectedHandler());
		child.setBusinessHandlerSet(serverhandlers);
		server.addchild(child);

		//把Server加入到管理器
		manager.addEndpointEntity(server);


		//模拟创建一个Client,要去连接上面的网关，使用上面账号密码
		CMPPClientEndpointEntity client = new CMPPClientEndpointEntity();
		client.setId("client");
		client.setHost("127.0.0.2,127.0.0.3,127.0.0.1");
		client.setPort(7891);
		client.setChartset(Charset.forName("utf-8"));
		client.setGroupName("test");
		client.setUserName("901782");
		client.setPassword("ICP");
		client.setReadLimit(0);
		child.setWriteLimit(100);
		client.setWindows((short)16);
		client.setVersion((short)48);
		client.setRetryWaitTimeSec((short)100);
		client.setUseSSL(false); //不使用SSL加密流量

		//这里是给Client增加一个处理业务:MessageReceiveHandler ，统计接收到的短信条数和速度，并打印到控制台。
		List<BusinessHandlerInterface> clienthandlers = new ArrayList<BusinessHandlerInterface>();
		clienthandlers.add(new MessageReceiveHandler());
		client.setBusinessHandlerSet(clienthandlers);

		//把Client加入到管理器
		manager.addEndpointEntity(client);

		//管理器打开端口
		manager.openAll();

		//主线程挂起，在控制台看实时打印的Log
		Thread.sleep(300000);

		//关闭所有连接和端口
		CMPPEndpointManager.INS.close();
	}
}
