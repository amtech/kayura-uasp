<%@ page language="java" contentType="text/html; charset=UTF-8"%>

<e:section name="title">登录页</e:section>

<e:section name="head">
<style type="text/css">
html, body { width: 100%; height: 100%; overflow: hidden; font-size: 12px; }
.login_negative { width: 910px; height: 420px; position: absolute; top: 50%; left: 50%; margin-left: -480px; margin-top: -310px; }
.login_header { height: 100px; overflow: hidden; margin: 0; padding: 0; }
.login_shadow { height: 416px; padding: 1px; background: #fff; border: #888888 1px solid; -moz-box-shadow: 3px 3px 4px #888888; -webkit-box-shadow: 3px 3px 4px #888888; box-shadow: 3px 3px 4px #888888; }
.login_imgshow { float: left; width: 600px; height: 416px; overflow: hidden; }
.login_window { float: left; width: 300px; }
</style>
</e:section>

<e:section name="body">
	<form id="loginForm" action="${root}/userlogin"
		method="post">
		<div class="login_negative">
			<div class="login_header" style="height: 100px; position: relative;">
				<img src='${root}/res/images/login/logo.png' style="position: absolute;" alt="" />
			</div>
			<div class="login_shadow">
				<div class="login_imgshow">
					<img src="${root}/res/images/login/img1.jpg" />
				</div>
				<div class="login_window">
					<img src="${root}/res/images/login/icon1.png" />
					<div style="padding: 30px 30px 10px 30px">
						<div style="margin-bottom: 10px">
							<e:textbox id="u" required="true"
								missingMessage="请输入用户名." novalidate="true" 
								style="width:100%;height:30px;padding:8px" iconCls="icon-man"
								iconWidth="30" prompt="用户名/手机号"
								value="${sessionScope['SPRING_SECURITY_LAST_USERNAME']}"></e:textbox>
						</div>
						<div style="margin-bottom: 10px">
							<e:textbox id="p" type="password" required="true"
								missingMessage="请输入密码." novalidate="true"
								style="width:100%;height:30px;padding:8px" iconCls="icon-lock"
								iconWidth="30" prompt="确认密码"></e:textbox>
						</div>
						<c:if test="${needvc == true}">
						<div style="margin-bottom: 10px">
							<e:textbox id="vcode" required="true" novalidate="true" missingMessage="请输入验证码."
								style="width:140px;height:30px;padding:8px" prompt="验证码"></e:textbox>
							<img src="${root}/res/vc" style="width:95px;height:30px;float: right;"
								onclick="this.src='${root}/res/vc?r=' + Math.random();" title="看不清？点击换一张。"/>
						</div>
						</c:if>
						<div style="margin-bottom: 20px">
							<label><input name="_spring_security_remember_me" type="checkbox" />记住用户名</label>
						</div>
						<div style="margin-bottom: 10px">
							<e:linkbutton text="进入系统" style="width:100%;height:30px;" onclick="login()"></e:linkbutton>
						</div>
						<div style="margin-bottom: 20px">
							<a href="javascript:void(0)">忘记登录密码？</a>
							<a href="javascript:void(0)" style="float: right; margin-right: 5px">自助注册</a>
						</div>
						<div id="login-error" style="width: 100%; color: red;">${message}</div>
					</div>
				</div>
			</div>
			<div style="float: right; margin: 15px">
				<c:if test="${runMode == 'dev'}">
				<a href="${root}/example/" target="_blank">开发示例库</a>
				</c:if>				
			</div>
		</div>
	</form>
	<script type="text/javascript">
		function login() {
			var f = $("#loginForm");
			if (f.form('enableValidation').form('validate')) {
				f.submit();
			}
		}
	</script>
</e:section>

<%@ include file="/shared/_simple.jsp"%>