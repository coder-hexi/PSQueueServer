package wjw.psqueue.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.wjw.efjson.JsonObject;

import wjw.psqueue.msg.ResAdd;
import wjw.psqueue.msg.ResData;
import wjw.psqueue.msg.ResSubStatus;
import wjw.psqueue.msg.ResultCode;

public class HttpRequestHandler extends SimpleChannelInboundHandler<Object> {
	private final App _app;

	public HttpRequestHandler(App app) {
		_app = app;
	}

	/**
	 * ��HTTP Header���ҵ��ַ�������,û�з��ַ���null
	 * 
	 * @param contentType
	 * @return
	 */
	String getCharsetFromContentType(String contentType) {
		if (null == contentType) {
			return null;
		}
		final int start = contentType.indexOf("charset=");
		if (start < 0) {
			return null;
		}
		String encoding = contentType.substring(start + 8);
		final int end = encoding.indexOf(';');
		if (end >= 0) {
			encoding = encoding.substring(0, end);
		}
		encoding = encoding.trim();
		if ((encoding.length() > 2) && ('"' == encoding.charAt(0)) && (encoding.endsWith("\""))) {
			encoding = encoding.substring(1, encoding.length() - 1);
		}
		return (encoding.trim());
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		//cause.printStackTrace();
		ctx.close();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
		FullHttpRequest request = (FullHttpRequest) msg;

		//����URL����
		QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri(), _app._conf.charsetDefaultCharset);
		Map<String, List<String>> parameters = queryStringDecoder.parameters();

		String charset = (null != parameters.get("charset")) ? parameters.get("charset").get(0) : null; //�ȴ�query�����charset
		Charset charsetObj = _app._conf.charsetDefaultCharset;
		if (null == charset) {
			if (null != request.headers().get("Content-Type")) {
				charset = getCharsetFromContentType(request.headers().get("Content-Type"));
				if (null == charset) {
					charset = _app._conf.defaultCharset;
				} else if (!charset.equalsIgnoreCase(_app._conf.defaultCharset)) { //˵����ѯ������ָ�����ַ���,������ȱʡ�ַ�����һ��
					charsetObj = Charset.forName(charset);
					queryStringDecoder = new QueryStringDecoder(request.getUri(), charsetObj);
					parameters = queryStringDecoder.parameters();
				}
			} else {
				charset = _app._conf.defaultCharset;
			}
		} else if (!charset.equalsIgnoreCase(_app._conf.defaultCharset)) { //˵����ѯ������ָ�����ַ���,������ȱʡ�ַ�����һ��
			charsetObj = Charset.forName(charset);
			queryStringDecoder = new QueryStringDecoder(request.getUri(), charsetObj);
			parameters = queryStringDecoder.parameters();
		}

		writeResponse(ctx, request, parameters, charsetObj);
	}

	private void writeResponse(ChannelHandlerContext ctx, FullHttpRequest request, Map<String, List<String>> parameters, Charset charsetObj) {
		//����GET������
		final String queueName = (null != parameters.get("qname")) ? parameters.get("qname").get(0) : null; // �������� 
		final String subName = (null != parameters.get("sname")) ? parameters.get("sname").get(0) : null; // �������� 
		final String opt = (null != parameters.get("opt")) ? parameters.get("opt").get(0) : null; //�������
		String data = (null != parameters.get("data")) ? parameters.get("data").get(0) : null; //��������
		final String pos_tmp = (null != parameters.get("pos")) ? parameters.get("pos").get(0) : null; //����λ�õ�

		long pos = 0;
		if (null != pos_tmp) {
			pos = Long.parseLong(pos_tmp);
		}

		//���ظ��û���Headerͷ��Ϣ
		FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.buffer(64));
		response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/plain;charset=" + charsetObj.name());
		response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
		response.headers().set(HttpHeaders.Names.CACHE_CONTROL, HttpHeaders.Values.NO_CACHE);

		ByteBuf respBuf = response.content(); //Buffer that stores the response content
		try {
			String jsonString;
			if (opt != null && queueName != null) {
				switch (opt) {
				case "add": {
					/* ���Ƚ���POST������Ϣ */
					if (request.getMethod().name().equalsIgnoreCase("POST")) {
						data = URLDecoder.decode(request.content().toString(charsetObj), charsetObj.name());
						ResAdd res = _app.add(queueName, data);
						jsonString = JsonObject.toJson(res);
					} else if (null != data) { //���POST���������ݣ���ȡURL��data������ֵ
						ResAdd res = _app.add(queueName, data);
						jsonString = JsonObject.toJson(res);
					} else {
						ResAdd res = new ResAdd(ResultCode.QUEUE_ADD_ERROR);
						jsonString = JsonObject.toJson(res);
					}
				}
					break;
				case "poll": {
					ResData res = _app.poll(queueName, subName);
					jsonString = JsonObject.toJson(res);
				}
					break;
				case "view": {
					ResData res = _app.view(queueName, pos);
					jsonString = JsonObject.toJson(res);
				}
					break;
				case "status": {
					ResSubStatus res = _app.statusForSub(queueName, subName);
					jsonString = JsonObject.toJson(res);
				}
					break;
				default: {
					jsonString = JsonObject.toJson(new ResData(ResultCode.CMD_INVALID));
				}
					break;
				}
			} else {
				jsonString = JsonObject.toJson(new ResData(ResultCode.QUEUE_NAME_INVALID));
			}

			respBuf.writeBytes(jsonString.getBytes(charsetObj));
		} catch (Throwable ex) {
			_app._log.error(ex.getMessage(), ex);
		}

		response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, respBuf.readableBytes());

		// Close the non-keep-alive connection after the write operation is done.
		boolean keepAlive = HttpHeaders.isKeepAlive(request);
		if (!keepAlive) {
			ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
		} else {
			ctx.writeAndFlush(response);
		}

	}
}
