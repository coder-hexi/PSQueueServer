package wjw.psqueue.server;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.FIELD)
public class QueueConf {
	public String name; //������

	public long dbFileMaxSize; //���������ļ�����С(�ֽ�)

	public QueueConf() {
		super();
	}

	public QueueConf(String name, long dbFileMaxSize) {
		super();
		this.name = name;
		this.dbFileMaxSize = dbFileMaxSize;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("[name=").append(name).append(", dbFileMaxSize=").append(dbFileMaxSize).append("]");
		return builder.toString();
	}
}
