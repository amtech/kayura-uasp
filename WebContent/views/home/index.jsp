<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<title>统一应用支撑平台</title>
<e:resources location="res" theme="${theme}" />
</head>
<e:body>
	<e:layoutunit region="north" style="height: 60px">
		<h2 style="padding-left: 10px; float: left;">统一应用支撑平台</h2>
	</e:layoutunit>
	<e:layoutunit region="south" style="height: 35px"></e:layoutunit>
	<e:layoutunit region="west" split="true" title="导航栏"
		style="width: 160px;">
		<e:accordion fit="true" border="false">
			<e:tab iconCls="icon-ok" title="常用模块">
				<ul>	
					<li><a href="###" onclick="juasp.openTab('账号管理', '${root}/admin/user/list')" >账号管理</a></li>
				</ul>
			</e:tab>
		</e:accordion>
	</e:layoutunit>
	<e:layoutunit region="center">
		<e:tabs id="mainTabs" fit="true" border="false">
			<e:tab id="homePage" title="首页" iconCls="icon-home"
				style="padding: 0px;overflow:hidden;"></e:tab>
		</e:tabs>
	</e:layoutunit>
</e:body>
</html>