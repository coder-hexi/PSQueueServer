package wjw.psqueue.server;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.FIELD)
public class QueueConf {
	public String name; //������

	public long capacity; //���е�����

	public QueueConf() {
		super();
	}

	public QueueConf(String name, long capacity) {
		super();
		this.name = name;
		this.capacity = capacity;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("[name=").append(name).append(", capacity=").append(capacity).append("]");
		return builder.toString();
	}
}
