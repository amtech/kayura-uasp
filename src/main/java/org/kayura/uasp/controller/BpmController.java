/**
 * Copyright 2015-2016 the original author or authors.
 * HomePage: http://www.kayura.org
 */
package org.kayura.uasp.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipInputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.engine.form.FormData;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.StartFormData;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.identity.Group;
import org.activiti.engine.identity.GroupQuery;
import org.activiti.engine.identity.User;
import org.activiti.engine.identity.UserQuery;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.Model;
import org.activiti.engine.repository.ModelQuery;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.activiti.explorer.util.XmlUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.kayura.activiti.expression.AssignmenteExpr;
import org.kayura.activiti.service.ActivitiService;
import org.kayura.activiti.vo.AssignItemVo;
import org.kayura.activiti.vo.BpmModelVo;
import org.kayura.activiti.vo.TaskVo;
import org.kayura.core.PostAction;
import org.kayura.core.PostResult;
import org.kayura.security.LoginUser;
import org.kayura.tags.easyui.types.TreeNode;
import org.kayura.tags.types.FormAttribute;
import org.kayura.type.GeneralResult;
import org.kayura.type.PageList;
import org.kayura.type.PageParams;
import org.kayura.type.Paginator;
import org.kayura.type.Result;
import org.kayura.uasp.po.BizForm;
import org.kayura.uasp.service.BpmService;
import org.kayura.utils.StringUtils;
import org.kayura.web.controllers.ActivitiController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * BpmController
 *
 * @author liangxia@live.com
 */
@Controller
public class BpmController extends ActivitiController {

	@Autowired
	private BpmService readerBpmService;

	@Autowired
	private BpmService writerBpmService;

	@Autowired
	private ActivitiService activitiService;

	@Autowired
	private AssignmenteExpr assignmenteExpr;

	@RequestMapping(value = "/modeler", method = RequestMethod.GET)
	public ModelAndView modeler() {

		ModelAndView mv = view("views/activiti/modeler");
		return mv;
	}

	@RequestMapping(value = "/bpm/biz/list", method = RequestMethod.GET)
	public ModelAndView bizList() {

		ModelAndView mv = view("views/bpm/biz-list");
		return mv;
	}

	@RequestMapping(value = "/bpm/biz/find", method = RequestMethod.POST)
	public void findBizList(Map<String, Object> map, HttpServletRequest req, String keyword) {

		LoginUser user = this.getLoginUser();
		postExecute(map, new PostAction() {

			@Override
			public void invoke(PostResult ps) {

				PageParams pp = ui.getPageParams(req);
				Result<PageList<BizForm>> r = readerBpmService.findBizForms(user.getTenantId(), keyword, pp);
				if (r.isSucceed()) {
					PageList<BizForm> pageList = r.getData();
					ps.setData(ui.putData(pageList));
				}
			}
		});
	}

	@RequestMapping(value = "/bpm/biz/new", method = RequestMethod.GET)
	public ModelAndView createBizForm() {

		BizForm entity = new BizForm();
		ModelAndView mv = view("views/bpm/biz-edit", entity);
		return mv;
	}

	@RequestMapping(value = "/bpm/biz/edit", method = RequestMethod.GET)
	public ModelAndView editBizForm(String id) {

		ModelAndView mv;
		Result<BizForm> r = readerBpmService.getBizFormsById(id);
		if (r.isSucceed()) {
			BizForm entity = r.getData();
			mv = view("views/bpm/biz-edit", entity);
		} else {
			mv = this.error(r);
		}
		return mv;
	}

	@RequestMapping(value = "/bpm/biz/navtree", method = RequestMethod.POST)
	public void bizNavTree(Map<String, Object> map) {

		LoginUser user = this.getLoginUser();
		postExecute(map, new PostAction() {

			@Override
			public void invoke(PostResult ps) {

				List<TreeNode> nodes = new ArrayList<TreeNode>();
				TreeNode root = new TreeNode("ROOT", "所有表单定义");
				root.setState(TreeNode.STATE_OPEN);
				root.addAttr("key", "");

				Result<List<BizForm>> r = readerBpmService.loadBizForms(user.getTenantId());
				if (r.isSucceed()) {

					List<BizForm> list = r.getData();
					for (BizForm b : list) {

						TreeNode bizNode = new TreeNode(b.getId(), b.getDisplayName());
						bizNode.addAttr("key", b.getProcessKey());

						// 设计 type=0
						TreeNode designNode = new TreeNode("DESIGN#" + b.getId(), "设计");
						designNode.addAttr("key", b.getProcessKey());
						designNode.addAttr("type", 0);
						bizNode.addNode(designNode);

						// 发布 type=1
						TreeNode releaseNode = new TreeNode("RELEASE#" + b.getId(), "发布");
						releaseNode.addAttr("key", b.getProcessKey());
						releaseNode.addAttr("type", 1);
						bizNode.addNode(releaseNode);

						// 挂起 type=2
						TreeNode suspendNode = new TreeNode("SUSPEND#" + b.getId(), "挂起");
						suspendNode.addAttr("key", b.getProcessKey());
						suspendNode.addAttr("type", 2);
						bizNode.addNode(suspendNode);

						root.addNode(bizNode);
					}
				}

				nodes.add(root);
				ps.setData(nodes);
			}
		});
	}

	@RequestMapping(value = "/bpm/biz/save", method = RequestMethod.POST)
	public void saveBizForm(Map<String, Object> map, BizForm model) {

		postExecute(map, new PostAction() {

			@Override
			public void invoke(PostResult ps) {

				GeneralResult r = null;

				if (StringUtils.isEmpty(model.getId())) {

					LoginUser user = getLoginUser();

					model.setStatus(BizForm.STATUS_DESIGN);
					model.setTenantId(user.getTenantId());

					r = writerBpmService.insertBizForm(model);
				} else {

					r = writerBpmService.updateBizForm(model);
				}

				if (r != null) {
					ps.setResult(r);
				}
			}
		});
	}

	@RequestMapping(value = "/bpm/biz/remove", method = RequestMethod.POST)
	public void removeBizForm(Map<String, Object> map, HttpServletRequest req, String ids) {

		postExecute(map, new PostAction() {

			@Override
			public void invoke(PostResult ps) {

				String[] idList = ids.split(",");
				for (String id : idList) {
					writerBpmService.deleteBizForm(id);
				}
			}
		});
	}

	@RequestMapping(value = "/bpm/task/list", method = RequestMethod.GET)
	public ModelAndView taskList() {

		ModelAndView mv = view("views/bpm/task-list");
		mv.addObject("jsid", RandomStringUtils.randomAlphabetic(4));
		return mv;
	}

	@RequestMapping(value = "/bpm/task/find", method = RequestMethod.POST)
	public void findTaskList(Map<String, Object> map, HttpServletRequest req, String keyword) {

		LoginUser user = this.getLoginUser();
		postExecute(map, new PostAction() {
			@Override
			public void invoke(PostResult ps) {

				PageParams pp = ui.getPageParams(req);
				TaskQuery taskQuery = taskService.createTaskQuery().taskTenantId(user.getTenantId())
						.taskCandidateOrAssigned(user.getIdentityId());

				if (StringUtils.isNotEmpty(keyword)) {
					taskQuery.taskNameLike("%" + keyword + "%");
				}

				long count = taskQuery.count();
				List<Task> list = taskQuery.listPage(pp.getOffset(), pp.getLimit());

				PageList<TaskVo> pageList = new PageList<TaskVo>(TaskVo.fromTasks(list), new Paginator(count));
				ps.setData(ui.putData(pageList));
			}
		});
	}

	@RequestMapping(value = "/bpm/task/claim", method = RequestMethod.POST)
	public void taskClaim(Map<String, Object> map, HttpServletRequest req, String id) {

		LoginUser user = this.getLoginUser();
		postExecute(map, new PostAction() {
			@Override
			public void invoke(PostResult ps) {
				taskService.claim(id, user.getIdentityId());
			}
		});
	}

	@RequestMapping(value = "/bpm/task/read", method = RequestMethod.GET)
	public ModelAndView taskRead(String id) {

		TaskFormData formData = formService.getTaskFormData(id);
		List<FormAttribute> props = convertFormData(formData);

		ModelAndView mv = view("views/bpm/task-form");
		mv.addObject("model", formData);
		mv.addObject("props", props);

		return mv;
	}

	@RequestMapping(value = "/bpm/task/handler", method = RequestMethod.POST)
	public void taskHandler(Map<String, Object> map, HttpServletRequest req, String id) {

		LoginUser user = this.getLoginUser();

		postExecute(map, new PostAction() {
			@Override
			public void invoke(PostResult ps) {

				TaskFormData formData = formService.getTaskFormData(id);

				List<FormProperty> formProperties = formData.getFormProperties();
				Map<String, String> formValues = new HashMap<String, String>();
				for (FormProperty formProperty : formProperties) {
					if (formProperty.isWritable()) {
						String value = req.getParameter(formProperty.getId());
						formValues.put(formProperty.getId(), value);
					}
				}

				identityService.setAuthenticatedUserId(user.getIdentityId());
				formService.submitTaskFormData(id, formValues);
			}
		});
	}

	@SuppressWarnings("unchecked")
	private List<FormAttribute> convertFormData(FormData formData) {

		List<FormAttribute> props = new ArrayList<FormAttribute>();
		for (FormProperty formProperty : formData.getFormProperties()) {

			FormAttribute fa = new FormAttribute(formProperty.getId(), formProperty.getName(), formProperty.getValue(),
					formProperty.getType().getName());
			fa.setDatePattern(Objects.toString(formProperty.getType().getInformation("datePattern")));
			fa.setItems((Map<String, String>) formProperty.getType().getInformation("values"));
			fa.setReadable(formProperty.isReadable());
			fa.setWriteable(formProperty.isWritable());
			fa.setRequired(formProperty.isRequired());

			props.add(fa);
		}
		return props;
	}

	@RequestMapping(value = "/bpm/proc/list", method = RequestMethod.GET)
	public ModelAndView processList() {

		return view("views/bpm/proc-list");
	}

	@RequestMapping(value = "/bpm/proc/inst", method = RequestMethod.GET)
	public ModelAndView processInst() {

		return view("views/bpm/proc-inst");
	}

	@RequestMapping(value = "/bpm/proc/find", method = RequestMethod.POST)
	public void findProcessList(Map<String, Object> map, HttpServletRequest req,
			@RequestParam(value = "t", required = false) Integer type, String key, String keyword) {

		LoginUser user = this.getLoginUser();
		postExecute(map, new PostAction() {
			@Override
			public void invoke(PostResult ps) {

				PageParams pp = ui.getPageParams(req);

				if (type == null || (type != 0 && type != 1 && type != 2)) {
					PageList<BpmModelVo> pageList = new PageList<BpmModelVo>(new ArrayList<BpmModelVo>(),
							new Paginator(0));
					ps.setData(ui.putData(pageList));

				} else {

					if (type == 0) {
						ModelQuery query = repositoryService.createModelQuery().modelTenantId(user.getTenantId());
						if (!StringUtils.isEmpty(key)) {
							query.modelKey(key);
						}

						long size = query.count();
						List<Model> list = query.listPage(pp.getOffset(), pp.getLimit());

						PageList<BpmModelVo> pageList = new PageList<BpmModelVo>(BpmModelVo.fromModels(list),
								new Paginator(size));
						ps.setData(ui.putData(pageList));
					} else if (type == 1 || type == 2) {

						ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery()
								.processDefinitionTenantId(user.getTenantId());
						if (!StringUtils.isEmpty(key)) {
							query.processDefinitionKey(key);
						}

						if (type == 1) {
							query.active();
						} else {
							query.suspended();
						}

						long size = query.count();
						List<ProcessDefinition> list = query.listPage(pp.getOffset(), pp.getLimit());

						PageList<BpmModelVo> pageList = new PageList<BpmModelVo>(BpmModelVo.fromDefinitions(list),
								new Paginator(size));
						ps.setData(ui.putData(pageList));
					}
				}
			}
		});
	}

	@RequestMapping(value = "/bpm/proc/remove", method = RequestMethod.POST)
	public void deleteProcess(Map<String, Object> map, HttpServletRequest req, @RequestParam("t") Integer type,
			String ids) {

		postExecute(map, new PostAction() {
			@Override
			public void invoke(PostResult ps) {

				String[] idList = ids.split(",");
				for (String id : idList) {
					if (type == 0) {
						repositoryService.deleteModel(id);
					} else {
						repositoryService.deleteDeployment(id, true);
					}
				}
			}
		});
	}

	@RequestMapping(value = "/bpm/proc/import", method = RequestMethod.POST)
	public void importModel(Map<String, Object> map, String key, MultipartFile file) {

		LoginUser user = this.getLoginUser();
		postExecute(map, new PostAction() {
			@Override
			public void invoke(PostResult ps) {

				try {
					String fileName = file.getOriginalFilename();
					if (fileName.endsWith(".bpmn20.xml") || fileName.endsWith(".bpmn")) {

						XMLInputFactory xif = XmlUtil.createSafeXmlInputFactory();
						InputStreamReader in = new InputStreamReader(new ByteArrayInputStream(file.getBytes()),
								"UTF-8");
						XMLStreamReader xtr = xif.createXMLStreamReader(in);
						BpmnModel bpmnModel = new BpmnXMLConverter().convertToBpmnModel(xtr);

						if (bpmnModel.getMainProcess() == null || bpmnModel.getMainProcess().getId() == null) {
							ps.setError("导入的模型无效。");
						} else {

							if (bpmnModel.getLocationMap().isEmpty()) {
								ps.setError("导入的模型无效。");
							} else {

								String processName = bpmnModel.getMainProcess().getName();
								if (StringUtils.isEmpty(processName)) {
									processName = bpmnModel.getMainProcess().getId();
								}

								Model model = repositoryService.newModel();

								ObjectNode metaInfo = new ObjectMapper().createObjectNode();
								metaInfo.put("name", processName);
								metaInfo.put("revision", 1);

								model.setMetaInfo(metaInfo.toString());
								model.setName(processName);
								model.setTenantId(user.getTenantId());
								model.setKey(key);

								repositoryService.saveModel(model);

								ObjectNode editorNode = new BpmnJsonConverter().convertToJson(bpmnModel);

								repositoryService.addModelEditorSource(model.getId(),
										editorNode.toString().getBytes("utf-8"));
							}
						}
					} else {
						ps.setError("导入的模型无效。");
					}
				} catch (Exception ex) {
					ps.setError("导入的模型无效。", ex);
				}
			}
		});
	}

	@RequestMapping(value = "/bpm/proc/new", method = RequestMethod.GET)
	public ModelAndView createModel(String key) {

		ModelAndView mv = view("views/bpm/proc-new");
		mv.addObject("key", key);
		return mv;
	}

	@RequestMapping(value = "/bpm/proc/new", method = RequestMethod.POST)
	public void saveNewModel(Map<String, Object> map, String key, String name, String desc) {

		LoginUser user = this.getLoginUser();
		postExecute(map, new PostAction() {

			@SuppressWarnings("deprecation")
			@Override
			public void invoke(PostResult ps) {

				try {
					ObjectMapper objectMapper = new ObjectMapper();
					ObjectNode editorNode = objectMapper.createObjectNode();
					editorNode.put("id", "canvas");
					editorNode.put("resourceId", "canvas");
					ObjectNode stencilSetNode = objectMapper.createObjectNode();
					stencilSetNode.put("namespace", "http://b3mn.org/stencilset/bpmn2.0#");
					editorNode.put("stencilset", stencilSetNode);

					Model model = repositoryService.newModel();

					ObjectNode metaInfo = new ObjectMapper().createObjectNode();
					metaInfo.put("name", name);
					metaInfo.put("revision", 1);
					metaInfo.put("description", desc);
					model.setMetaInfo(metaInfo.toString());

					model.setName(name);
					model.setTenantId(user.getTenantId());
					model.setKey(key);

					repositoryService.saveModel(model);
					repositoryService.addModelEditorSource(model.getId(), editorNode.toString().getBytes("utf-8"));

					ps.setData(model.getId());

				} catch (Exception ex) {
					ps.setException(ex);
				}
			}
		});
	}

	@RequestMapping(value = "/bpm/proc/upload", method = RequestMethod.POST)
	public void uploadProcess(Map<String, Object> map, MultipartFile file) {

		LoginUser user = this.getLoginUser();
		postExecute(map, new PostAction() {
			@Override
			public void invoke(PostResult ps) {

				String fileName = file.getOriginalFilename();
				try {
					InputStream inputStream = file.getInputStream();
					String extension = FilenameUtils.getExtension(fileName);
					DeploymentBuilder deployment = repositoryService.createDeployment();
					deployment.tenantId(user.getTenantId()).name("").category("");
					if (extension.equals("zip") || extension.equals("bar")) {
						ZipInputStream zipInputStream = new ZipInputStream(inputStream);
						deployment.addZipInputStream(zipInputStream);
					} else {
						deployment.addInputStream(fileName, inputStream);
					}
					deployment.deploy();

					ps.setSuccess("流程定义布署成功。");
				} catch (Exception e) {
					logger.error(e.getMessage());
					ps.setSuccess("流程定义布署失败。" + e.getMessage());
				}
			}
		});
	}

	@RequestMapping(value = "/bpm/proc/deploy", method = RequestMethod.POST)
	public void deployProcess(Map<String, Object> map, String modelId) {

		LoginUser user = this.getLoginUser();
		postExecute(map, new PostAction() {
			@Override
			public void invoke(PostResult ps) {

				try {
					Model modelNode = repositoryService.getModel(modelId);

					byte[] extra = repositoryService.getModelEditorSourceExtra(modelId);
					ByteArrayInputStream extraSteam = new ByteArrayInputStream(extra);

					JsonNode jsonNode = new ObjectMapper().readTree(repositoryService.getModelEditorSource(modelId));
					BpmnModel bpmnModel = new BpmnJsonConverter().convertToBpmnModel(jsonNode);

					String processName = modelNode.getName() + ".bpmn20.xml";
					String pngName = modelNode.getName() + ".png";

					repositoryService.createDeployment().name(modelNode.getName()).tenantId(user.getTenantId())
							.addInputStream(pngName, extraSteam).addBpmnModel(processName, bpmnModel).deploy();
				} catch (Exception ex) {
					ps.setException(ex);
				}
			}
		});
	}

	/**
	 * 
	 * @param response
	 * @param id
	 * @param status
	 *            状态：0 设计；1 发布；2 挂起；
	 * @param resType
	 *            资源类型：1 流程；2 图型;
	 * @throws IOException
	 */
	@RequestMapping(value = "/bpm/proc/res")
	@ResponseBody
	public void viewProcess(HttpServletResponse response, String id, @RequestParam("s") Integer status,
			@RequestParam("t") Integer resType) throws IOException {
		try {
			LoginUser user = this.getLoginUser();
			if (status == 1 || status == 2) {

				ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
						.processDefinitionTenantId(user.getTenantId()).processDefinitionId(id).singleResult();
				String resName = pd.getResourceName();
				if (resType == 2) {
					resName = pd.getDiagramResourceName();
				}
				InputStream stream = repositoryService.getResourceAsStream(pd.getDeploymentId(), resName);

				byte[] buffer = new byte[4096];
				int bytesRead = -1;
				while ((bytesRead = stream.read(buffer)) != -1) {
					response.getOutputStream().write(buffer, 0, bytesRead);
				}
			} else if (status == 0) {

				Model model = repositoryService.getModel(id);
				byte[] buffer = null;
				if (model.hasEditorSource() && resType == 1) {
					JsonNode jsonNode = new ObjectMapper().readTree(repositoryService.getModelEditorSource(id));
					BpmnModel bpmnModel = new BpmnJsonConverter().convertToBpmnModel(jsonNode);
					BpmnXMLConverter bpmnXMLConverter = new BpmnXMLConverter();
					buffer = bpmnXMLConverter.convertToXML(bpmnModel);
				} else if (model.hasEditorSourceExtra() && resType == 2) {
					buffer = repositoryService.getModelEditorSourceExtra(id);
				}
				if (buffer != null) {
					response.getOutputStream().write(buffer);
				}
			}
		} catch (Exception ex) {
			logger.error("显示资源内容时发生异常。", ex);
		}
	}

	@RequestMapping(value = "/bpm/proc/form/start", method = RequestMethod.GET)
	public ModelAndView startForm(String id) {

		StartFormData formData = formService.getStartFormData(id);
		List<FormAttribute> props = convertFormData(formData);

		ModelAndView mv = view("views/bpm/start-form");
		mv.addObject("model", formData);
		mv.addObject("props", props);

		return mv;
	}

	@RequestMapping(value = "/bpm/proc/start", method = RequestMethod.POST)
	public void startProcess(Map<String, Object> map, HttpServletRequest request, String processDefinitionId) {

		LoginUser user = this.getLoginUser();
		postExecute(map, new PostAction() {

			@Override
			public void invoke(PostResult ps) {

				StartFormData formData = formService.getStartFormData(processDefinitionId);
				List<FormProperty> formProperties = formData.getFormProperties();
				Map<String, String> formValues = new HashMap<String, String>();

				for (FormProperty formProperty : formProperties) {
					if (formProperty.isWritable()) {
						String value = request.getParameter(formProperty.getId());
						formValues.put(formProperty.getId(), value);
					}
				}

				identityService.setAuthenticatedUserId(user.getIdentityId());
				formService.submitStartFormData(processDefinitionId, formValues);
			}
		});
	}

	/**
	 * @param types
	 *            D 部门, P 岗位, G 群组, R 角色, T 表达式分类, E 表达式。使用 , 分隔.
	 * @param keyword
	 *            过滤名称关键字.
	 */
	@RequestMapping(value = "/bpm/group/find", method = RequestMethod.GET)
	public void findGroups(Map<String, Object> map, HttpServletRequest req, String type, String keyword) {

		postExecute(map, new PostAction() {
			@Override
			public void invoke(PostResult ps) {

				List<AssignItemVo> voItems = new ArrayList<AssignItemVo>();

				if (type.equals("T")) {

					voItems.add(new AssignItemVo("EXPRESSION", "内部表达式", "T"));
				} else if (type.equals("E")) {

					HashMap<String, String> exprs = assignmenteExpr.getItems();
					for (String key : exprs.keySet()) {
						AssignItemVo assign = new AssignItemVo();
						assign.setValue("${" + key + "}");
						assign.setName(exprs.get(key) + "（" + assign.getValue() + "）");
						assign.setType("T");
						voItems.add(assign);
					}
				} else {
					GroupQuery query = identityService.createGroupQuery().groupType(type);
					if (StringUtils.isNotEmpty(keyword)) {
						query.groupNameLike("%" + keyword + "%");
					}
					List<Group> list = query.list();
					voItems = convertAssignGroup(list);
				}
				ps.setData(voItems);
			}
		});
	}

	@RequestMapping(value = "/bpm/user/find", method = RequestMethod.GET)
	public void findUsers(Map<String, Object> map, HttpServletRequest req, String gid, String keyword) {

		postExecute(map, new PostAction() {
			@Override
			public void invoke(PostResult ps) {

				List<AssignItemVo> voItems;

				if (gid.equals("EXPRESSION")) {
					voItems = new ArrayList<AssignItemVo>();
					HashMap<String, String> exprs = assignmenteExpr.getItems();
					for (String key : exprs.keySet()) {
						AssignItemVo assign = new AssignItemVo();
						assign.setValue("${" + key + "}");
						assign.setName(exprs.get(key) + "（" + assign.getValue() + "）");
						assign.setType("T");
						voItems.add(assign);
					}
				} else {
					UserQuery query = identityService.createUserQuery().memberOfGroup(gid);
					if (StringUtils.isNotEmpty(keyword)) {
						query.userFullNameLike("%" + keyword + "%");
					}
					List<User> list = query.list();
					voItems = convertAssignUser(list);
				}

				ps.setData(voItems);
			}
		});
	}

	private List<AssignItemVo> convertAssignGroup(List<Group> list) {
		List<AssignItemVo> items = new ArrayList<AssignItemVo>();
		for (Group g : list) {
			items.add(new AssignItemVo(g.getId(), g.getName(), g.getType()));
		}
		return items;
	}

	private List<AssignItemVo> convertAssignUser(List<User> list) {
		List<AssignItemVo> items = new ArrayList<AssignItemVo>();
		for (User u : list) {
			items.add(new AssignItemVo(u.getId(), u.getLastName() + "（" + u.getFirstName() + "）", "U"));
		}
		return items;
	}

	/**
	 * 用于返回指派候选者的显示名与类型.
	 * 
	 * @param ids
	 *            指派者Id集,或为用户/群组/特殊表达式.
	 * @param type
	 *            U 用户; G 群组;
	 */
	@RequestMapping(value = "/bpm/assign/find", method = RequestMethod.GET)
	public void findAssignItems(Map<String, Object> map, HttpServletRequest req, String ids,
			@RequestParam("t") String type) {

		postExecute(map, new PostAction() {
			@Override
			public void invoke(PostResult ps) {

				List<String> idList = Arrays.asList(ids.split(","));

				List<AssignItemVo> list;
				if (type.equals("U")) {
					list = activitiService.loadAssignUsersByIds(idList);
				} else if (type.equals("G")) {
					list = activitiService.loadAssignGroupsByIds(idList);
				} else {
					list = new ArrayList<AssignItemVo>();
				}

				ps.setData(list);
			}
		});
	}

}
