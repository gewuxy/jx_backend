<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <title>个人中心 - 头像设置</title>
    <%@include file="/WEB-INF/include/page_context.jsp" %>
    <meta content="width=device-width, initial-scale=1.0, user-scalable=no" name="viewport">
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1" />
    <link rel="stylesheet" href="${ctxStatic}/css/global.css">
    <link rel="stylesheet" href="${ctxStatic}/css/menu.css">
    <link rel="stylesheet" href="${ctxStatic}/css/perfect-scrollbar.min.css">
    <link rel="stylesheet" href="${ctxStatic}/css/animate.min.css" type="text/css" />
    <link rel="stylesheet" href="${ctxStatic}/css/style.css">

</head>


<body>
<div id="wrapper">
    <%@include file="/WEB-INF/include/header_zh_CN.jsp" %>
    <div class="admin-content bg-gray" >
        <div class="page-width clearfix">
            <div class="user-module clearfix">
                <div class="row clearfix">
                    <div class="col-lg-4">
                        <%@include file="left.jsp"%>
                    </div>
                    <div class="col-lg-8">
                        <%@include file="user_include.jsp" %>
                        <div class="user-content user-content-levelHeight item-radius">
                            <div class="user-portrait-upload">
                                <div class="user-portrait-area item-radius">
                                    <p>Upload Head-Portrait</p>
                                </div>
                                <p>Please select a jpg/png image less than 1M.</p>
                                <input type="file" id="headimg" style="display:none" name="file" onchange="toUpload()">
                                <input href="#" type="button"  class="button login-button buttonBlue last" onclick="headimg.click()" value="Upload Head-Portrait">
                            </div>
                        </div>
                    </div>
                    <%@include file="/WEB-INF/include/footer_zh_CN.jsp"%>
                </div>
            </div>
        </div>
    </div>

</div>

<script src="${ctxStatic}/js/ajaxfileupload.js"></script>
<script>

    $("#config_2").parent().attr("class","cur");
    var fileUploadUrl = "${ctx}/mgr/user/updateAvatar";
    $(function () {

    });

    function toUpload(){
        var filename = $("#headimg").val();
        var extStart = filename.lastIndexOf(".");
        var ext = filename.substring(extStart,filename.length).toUpperCase();
        if(ext != ".BMP" && ext != ".PNG" && ext != ".JPG" && ext != ".JPEG"&&ext != ".TIFF"){
            layer.msg("Please select a jpg/png image");
        }else{
            //ajaxFileUpload的回调要改为data.code，之前是data.status，会报错
            upload("headimg","headimg",1*1024*1024,uploadHandler);
        }
    }

    function uploadHandler(result){
        $("#image").attr("src",result.data);
        $("#head_img").attr("src",result.data);
    }

</script>

</body>
</html>



