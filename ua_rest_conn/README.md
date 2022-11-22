# UserApp REST Connector

The UserApp REST connector provides methods to allow you to start, manage, and monitor workflows on hosts running the UserApp RIS.war from IdMUnit tests.

## Operations

---

### TestConnection

Test the connection to the configured UserApp REST connection.

---

### StartWorkflow

Start an instance of the specified workflow.

#### Params

- **workflowIdentifier** - An identifier to reference the started workflow in other operations.
- **workflowDn** - The DN of the workflow you would like to start.
- **workflowRecipient** - The DN of the recipient for the workflow.
- **<*>** - All other fields are passed as data items to the workflow.

---

### ApproveWorkflow

Approve all workitems for the specified workflow

#### Params

- **workflowIdentifier** - An identifier specified during a StartWorkflow or captured using CaptureWorkflow.
- **<*>** - All other fields are passed as data items to the workflow.

---

### DenyWorkflow

Deny all workitems in a workflow.

#### Params

- **workflowIdentifier** - An identifier specified during a StartWorkflow or captured using CaptureWorkflow.
- **<*>** - All other fields are passed as data items to the workflow.

---

### CheckWorkflowStatus

Check the workflow approval and workflow status for the specified workflow.

#### Params

- **workflowIdentifier** - An identifier specified during a StartWorkflow or captured using CaptureWorkflow.
- **workflowApprovalStatus** - The approval status value to match. e.g. Denied, Approved
- **workflowProcessStatus** - The process status value to match. e.g. Running, Completed

---

### PreCaptureWorkflow

Capture system state before a workflow is started by an external stimulus. CaptureWorkflow must be called after the workflow is started and before other connector operations are performed.

#### Params

- **workflowIdentifier** - An identifier to reference the captured workflow in other operations.
- **workflowDn** - The DN of the workflow you would like to start.

---

### CaptureWorkflow

Capture system state after a workflow is started by an external stimulus. This must be called after PreCaptureWorkflow and before other connector operations are performed.

#### Params

- **workflowIdentifier** - An identifier to reference the captured workflow in other operations.

---

### CaptureWorkflowFilteredWorkItem

Capture a workflow process based off of a workitem filter.

#### Params

- **workflowIdentifier** - An identifier to reference the captured workflow in other operations.
- **workflowFilter** - A filter that will be used during the rest call. For Example "recipient=cn=jtrivir,ou=users,o=trivir". This command can take multiple filters, but if there are many results in any given filter, you may get a socket closed exception.

---

### CaptureWorkflowFilteredProcess

Capture a workflow based off of a process filter.

#### Params

- **workflowIdentifier** - An identifier to reference the captured workflow in other operations.
- **workflowFilter** - A filter that will be used during the rest call. For Example "recipient=cn=jtrivir,ou=users,o=trivir". This command can take multiple filters, but if there are many results in any given filter, you may get a socket closed exception.

---

## Configuration

To configure this connector you need to specify a server, user, and a password.

```xml
<connection>
	<name>UserAppRest</name>
	<description>Connector to UserApp via RIS.war</description>
	<type>com.trivir.idmunit.connector.UserAppREST</type>
	<server>http://127.0.0.1:8080/RIS</server>
	<user>cn=admin,o=services</user>
	<password>B2vPD2UsfKc=</password>
	<multiplier/>
	<substitutions/>
	<data-injections/>
</connection>
```

