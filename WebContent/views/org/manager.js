
jctx = (function(win, $) {
	
	var rootPath = "";
	var isInit = true;
	var isfirst = true;
	var searchvalue = "";
	var actions = {
			addcompany : false,
			adddepart : false,
			addposition : false,
			remove : false
		};
	
	function _init(path){
		
		rootPath = path;
		
		$('#tv').tree({
			url : rootPath + "/org/tree.json",
			onClick : function(node) {
				_clickNode(node);
			},
			onLoadSuccess : function(node, data){
				if(isInit) {
					$(this).tree("collapseAll");
					var root = $(this).tree('find', "ROOT");
					$(this).tree("expand", root.target);
					isInit = false;
				}
			},
			onContextMenu: function(e, node){
				e.preventDefault();
				$(this).tree('select', node.target);
				_clickNode(node);
				$('#mm').menu('show', { left: e.pageX, top: e.pageY });
			}
		});
		
		$("#search").textbox({
			iconCls:'icon-search',
			onChange : function(n, o){
				_search(n);
			}
		});
		
		$("#search").textbox('addClearBtn');
	}
	
	function _initActions(){
		
		actions.addcompany = false;
		actions.adddepart = false;
		actions.addposition = false;
		actions.remove = false;
	}
	
	function _applyActions(_actions){

		$('#mm').menu((_actions.addcompany?'showItem':'hideItem'), $("#mmaddcompany"));
		$('#mm').menu((_actions.adddepart?'showItem':'hideItem'), $("#mmadddepart"));
		$('#mm').menu((_actions.addposition?'showItem':'hideItem'), $("#mmaddposition"));
		$('#mm').menu((_actions.remove?'showItem':'hideItem'), $("#mmremove"));

		$("#tbaddcompany").linkbutton(_actions.addcompany?'enable':'disable');
		$("#tbadddepart").linkbutton(_actions.adddepart?'enable':'disable');
		$("#tbaddposition").linkbutton(_actions.addposition?'enable':'disable');
	}

	function _clickNode(node) {

		selectNode = node;
		_initActions();

		// 0 根, 1 公司, 2 部门, 3 岗位;
		var type = node.attributes['type'];
		if (type == 0) {
			actions.addcompany = true;
		} else if (type == 1) {
			actions.addcompany = true;
			actions.adddepart = true;
		} else if (type == 2) {
			actions.adddepart = true;
			actions.addposition = true;
		}

		if (type != 0 && node.children.length == 0) {
			actions.remove = true;
		}

		_applyActions(actions);
		_findItems(node.id);
	}

	function _search(value) {

		searchvalue = value;
		_findItems(selectNode.id);
	}

	function _findItems(nodeId) {

		var id = nodeId;
		if (nodeId == "ROOT") {
			id = "";
		}

		if (isfirst) {

			$('#tg').datagrid({
				url : rootPath + "/org/find.json",
				queryParams : {
					"id" : id
				}
			});
		} else {

			$('#tg').datagrid('load', {
				"id" : id,
				"keyword" : searchvalue
			});
			$('#tg').datagrid('unselectAll');
		}

		isfirst = false;
	}
	
	function _editCompany(id){
		
		var url = rootPath + "/org/company";
		
		if(juasp.isEmpty(id)) {
			var pid = "";
			if(selectNode.id != "ROOT" && selectNode.id.length == 32) {
				pid = selectNode.id;
			}
			url = url + "/new?pid=" + pid + "&pname=" + selectNode.text;
		} else {
			url = url + "?id=" + id;
		}

		$('#tv').tree('expand', selectNode.target);
		juasp.openWin({
			url : url,
			width : "450px",
			height : "500px",
			title : "公司信息",
			onClose : function(r) {
				if (r.result == 1) {
					if(juasp.isEmpty(id)) {
						$('#tv').tree('append', {
							parent : selectNode.target,
							data : [ {
								id : r.id,
								iconCls : 'icon-company',
								text : r.text,
								attributes : { type : 1 },
								children : []
							} ]
						});
					} else {
						$('#tv').tree('update', {
							target : node.target,
							text : r.text
						});
					}
				}
			}
		});
		
	}

	function _addCompany(){
		_editCompany();
	}
	
	function _editDepart(id){
		
		var url = rootPath + "/org/depart";
		
		if(juasp.isEmpty(id)) {
			var pid = selectNode.id;
			var type = selectNode.attributes.type;
			var pname = selectNode.text;
			url = url + "/new?pid=" + selectNode.id + "&t=" + type + "&pname=" + selectNode.text;
		} else {
			url = url + "?id=" + id;
		}

		$('#tv').tree('expand', selectNode.target);
		juasp.openWin({
			url : url,
			width : "450px",
			height : "500px",
			title : "部门信息",
			onClose : function(r) {
				if (r.result == 1) {
					if(juasp.isEmpty(id)) {
						$('#tv').tree('append', {
							parent : selectNode.target,
							data : [ {
								id : r.id,
								iconCls : 'icon-depart',
								text : r.text,
								attributes : { type : 2 },
								children : []
							} ]
						});
					} else {
						$('#tv').tree('update', {
							target : node.target,
							text : r.text
						});
					}
				}
			}
		});
	}

	function _addDepart(){
		
		_editDepart();
	}
	
	function _addPosition(id){
		
		var url = rootPath + "/org/position";
		
		if(juasp.isEmpty(id)) {
			var pid = selectNode.id;
			var type = selectNode.attributes.type;
			var pname = selectNode.text;
			url = url + "/new?pid=" + selectNode.id + "&t=" + type + "&pname=" + selectNode.text;
		} else {
			url = url + "?id=" + id;
		}

		$('#tv').tree('expand', selectNode.target);
		juasp.openWin({
			url : url,
			width : "450px",
			height : "500px",
			title : "公司信息",
			onClose : function(r) {
				if (r.result == 1) {
					if(juasp.isEmpty(id)) {
						$('#tv').tree('append', {
							parent : selectNode.target,
							data : [ {
								id : r.id,
								iconCls : 'icon-position',
								text : r.text,
								attributes : { type : 3 },
								children : []
							} ]
						});
					} else {
						$('#tv').tree('update', {
							target : node.target,
							text : r.text
						});
					}
				}
			}
		});
	}
	
	function _edit(){
		
		if(selectNode != null){
			
			// 0 根节点; 1 公司; 2 部门; 3 岗位;
			var type = selectNode.attributes.type;
			var id = selectNode.id;
			
			if(type == 1) {
				_editCompany(id);
			} else if(type == 2) {
				_editDepart(id);
			} else if (type == 3) {
				
			}
		}
		
	}
	
	function _removeOrg(){

		if(selectNode != null) {
			
			juasp.confirm("是否确认删除 " + selectNode.text + " 项。", function(r){
				if(r == true){
					// 0 根节点; 1 公司; 2 部门; 3 岗位;
					var type = selectNode.attributes.type;
					var id = selectNode.id;
					
					juasp.post(rootPath + "/org/remove", { id: id, t: type }, {
						success: function(r){
							$('#tv').tree('remove', { target : node.target });
							juasp.infotips(selectNode.text + " 已经被删除。");
						}
					});
				}
			});
			
		}
	}
	
	return {
		init : _init,
		search : _search,
		edit : _edit,
		removeOrg : _removeOrg,
		addCompany : _addCompany,
		addDepart : _addDepart
	}

}(window, jQuery));