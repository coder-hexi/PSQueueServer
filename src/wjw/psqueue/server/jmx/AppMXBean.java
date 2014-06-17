package wjw.psqueue.server.jmx;


import wjw.psqueue.msg.ResAdd;
import wjw.psqueue.msg.ResData;
import wjw.psqueue.msg.ResList;
import wjw.psqueue.msg.ResQueueStatus;
import wjw.psqueue.msg.ResSubStatus;
import wjw.psqueue.msg.ResultCode;
import wjw.psqueue.server.jmx.annotation.Description;
import wjw.psqueue.server.jmx.annotation.ManagedOperation;

public interface AppMXBean {
	@ManagedOperation(description = "���������ļ�")
	public ResultCode gc();

	@ManagedOperation(description = "��������")
	public ResultCode createQueue(@Description(name = "queueName", 
	description = "������") String queueName,
	@Description(name = "user", description = "�û���") final String user,
	@Description(name = "pass", description = "����") final String pass);

	@ManagedOperation(description = "����ָ�����е�ָ��������")
	public ResultCode createSub(@Description(name = "queueName", description = "������") String queueName, 
			@Description(name = "subName", description = "��������") String subName,
	@Description(name = "user", description = "�û���") final String user,
	@Description(name = "pass", description = "����") final String pass);

	@ManagedOperation(description = "ɾ��ָ������")
	public ResultCode removeQueue(@Description(name = "queueName", description = "������") String queueName,
			@Description(name = "user", description = "�û���") final String user,
			@Description(name = "pass", description = "����") final String pass);

	@ManagedOperation(description = "ɾ��ָ�����е�ָ��������")
	public ResultCode removeSub(@Description(name = "queueName", description = "������") String queueName, 
			@Description(name = "subName", description = "��������") String subName,
			@Description(name = "user", description = "�û���") final String user,
			@Description(name = "pass", description = "����") final String pass);

	@ManagedOperation(description = "����״̬")
	public ResQueueStatus status(@Description(name = "queueName", description = "������") String queueName);

	@ManagedOperation(description = "����״̬")
	public ResSubStatus statusForSub(@Description(name = "queueName", description = "������") String queueName, 
			@Description(name = "subName", description = "��������") String subName);

	@ManagedOperation(description = "��ȡȫ��������")
	public ResList queueNames();

	@ManagedOperation(description = "��ȡָ����������ȫ��������")
	public ResList subNames(@Description(name = "queueName", description = "������") String queueName);

	@ManagedOperation(description = "���ö���")
	public ResultCode resetQueue(@Description(name = "queueName", 
	    description = "������") String queueName,
			@Description(name = "user", description = "�û���") final String user,
			@Description(name = "pass", description = "����") final String pass);

	@ManagedOperation(description = "�������-��ָ������")
	public ResAdd add(@Description(name = "queueName", description = "������") String queueName, 
			@Description(name = "data", description = "����") final String data);

	@ManagedOperation(description = "��ȡ����-��ָ������")
	public ResData poll(@Description(name = "queueName", description = "������") String queueName, 
			@Description(name = "subName", description = "��������") String subName);

	@ManagedOperation(description = "�鿴ָ����������")
	public ResData view(@Description(name = "queueName", description = "������") String queueName, 
			@Description(name = "pos", description = "�鿴��λ��") final long pos);
}
