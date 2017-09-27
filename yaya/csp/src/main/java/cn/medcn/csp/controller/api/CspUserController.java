package cn.medcn.csp.controller.api;

import cn.medcn.common.Constants;
import cn.medcn.common.ctrl.BaseController;
import cn.medcn.common.ctrl.FilePath;
import cn.medcn.common.excptions.SystemException;
import cn.medcn.common.service.JPushService;
import cn.medcn.common.supports.FileTypeSuffix;
import cn.medcn.common.utils.*;
import cn.medcn.common.utils.StringUtils;
import cn.medcn.csp.security.Principal;
import cn.medcn.csp.security.SecurityUtils;
import cn.medcn.user.dto.CspUserInfoDTO;
import cn.medcn.user.model.AppUser;
import cn.medcn.user.model.BindInfo;
import cn.medcn.user.model.CspUserInfo;
import cn.medcn.user.service.AppUserService;
import cn.medcn.user.service.CspUserService;
import com.google.common.collect.Maps;
import org.apache.xmlbeans.impl.xb.xmlconfig.Extensionconfig;
import org.jdom.JDOMException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.awt.color.ColorSpace;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by Liuchangling on 2017/9/27.
 */
@Controller
@RequestMapping(value = "/api/user")
public class CspUserController extends BaseController {
    @Autowired
    protected CspUserService cspUserService;

    @Autowired
    protected RedisCacheUtils<String> redisCacheUtils;

    @Autowired
    protected AppUserService appUserService;

    @Autowired
    private JPushService jPushService;


    @Value("${csp.file.upload.base}")
    protected String uploadBase;

    @Value("${csp.file.base}")
    protected String fileBase;


    /**
     * 注册csp账号
     *
     * @param userInfo
     */
    @RequestMapping("/register")
    @ResponseBody
    public void register(CspUserInfo userInfo) {
        if (userInfo == null) {
            error(SpringUtils.getMessage("error.param"));
        } else {
            try {
                cspUserService.register(userInfo);
            } catch (Exception e) {
                new SystemException("user info can not be null");
            }
            success();
        }
    }

    /**
     * 邮箱+密码、手机+验证码登录 、第三方账号登录
     * type 1=微信 2=微博 3=Facebook 4=Twitter 5=YaYa医师 6=手机 7=邮箱
     * 登录检查用户是否存在csp账号，如果存在，登录成功返回用户信息；
     * 反之，根据客户端传过来的第三方信息，保存到数据库，再返回登录成功及用户信息
     *
     * @param username     邮箱
     * @param password     密码
     * @param thirdPartyId 第三方平台id
     * @param mobile       手机
     * @param captcha      验证码
     * @param nickName     昵称
     * @param userInfoDTO  第三方用户信息
     * @return
     */
    @RequestMapping("/login")
    @ResponseBody
    public void login(String username, String password, Integer thirdPartyId,
                      String mobile, String captcha, String nickName,
                      CspUserInfoDTO userInfoDTO, HttpServletRequest request) {

        if (thirdPartyId == null || thirdPartyId == 0) {
            error(SpringUtils.getMessage("user.empty.ThirdPartyId"));
        }

        // 第三方平台id
        int type = thirdPartyId.intValue();

        if (type == BindInfo.Type.EMAIL.getTypeId()) {
            // 邮箱登录
            loginByEmail(username, password, request);

        } else if (type == BindInfo.Type.MOBILE.getTypeId()) {
            // 手机登录
            loginByMobile(mobile, captcha, request);

        } else if (type <= BindInfo.Type.YaYa.getTypeId()) {
            if (userInfoDTO != null) {
                userInfoDTO.setThirdPartyId(type);
            }
            // 第三方账号登录 含YaYa医师登录
            loginByThirdParty(userInfoDTO, request);
        }

    }

    /**
     * 邮箱登录
     *
     * @param username
     * @param password
     */
    protected void loginByEmail(String username, String password, HttpServletRequest request) {
        if (StringUtils.isEmpty(username)) {
            error(SpringUtils.getMessage("user.empty.username"));
        }
        if (StringUtils.isEmpty(password)) {
            error(SpringUtils.getMessage("user.empty.password"));
        }

        CspUserInfo userInfo = cspUserService.findByLoginName(username);
        if (userInfo == null) {
            error(SpringUtils.getMessage("user.error.nonentity"));
        }
        // 用户输入密码是否正确
        if (!MD5Utils.md5(password).equals(userInfo.getPassword())) {
            error(SpringUtils.getMessage("user.error.password"));
        }
        // 登录成功，返回用户信息
        loginSuccess(userInfo, userInfo.getToken(), request);

    }


    /**
     * 发送手机验证码
     * type 发送短信验证码模板内容区分 0=登录 1=绑定
     */
    @RequestMapping("/sendCaptcha")
    @ResponseBody
    public void sendCaptcha(String mobile, Integer type) {
        try {
            cspUserService.sendCaptcha(mobile, type);
            success();
        } catch (Exception e) {
            error(SpringUtils.getMessage(e.getMessage()));
        }
    }


    /**
     * 修改头像
     *
     * @param file
     * @return
     */
    @RequestMapping(value = "/updateAvatar", method = RequestMethod.POST)
    public String updateAvatar(@RequestParam(value = "file", required = false) MultipartFile file) {
        if (file == null) {
            return error(local("upload.error.null"));
        }
        //相对路径
        String relativePath = FilePath.PORTRAIT.path + File.separator;
        //文件保存路径
        String savePath = uploadBase + relativePath;
        File dir = new File(savePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        //头像后缀
        String suffix = FileTypeSuffix.IMAGE_SUFFIX_JPG.suffix;
        String fileName = UUIDUtil.getNowStringID() + "." + suffix;
        String avatarFile = savePath + fileName;
        File saveFile = new File(avatarFile);
        try {
            file.transferTo(saveFile);
        } catch (IOException e) {
            e.printStackTrace();
            return error(local("upload.avatar.err"));
        }

        //更新用户头像
        CspUserInfo info = new CspUserInfo();
        info.setId(SecurityUtils.get().getId());
        info.setAvatar(relativePath + fileName);
        cspUserService.updateByPrimaryKeySelective(info);
        return success(fileBase + relativePath + fileName);
    }


    /**
     * 更新个人信息中的姓名和简介
     */
    @RequestMapping("/updateInfo")
    public String updateInfo(CspUserInfo info) {
       info.setId(SecurityUtils.get().getId());
       cspUserService.updateByPrimaryKeySelective(info);
        return success();
    }


    /**
     * 修改密码
     * 用户登录时后台会将邮箱地址返回给前端，前端根据有无返回邮箱地址判断是否需要绑定邮箱
     * 请求此接口，说明用户已绑定邮箱
     */
    @RequestMapping("resetPwd")
    public String resetPwd(String oldPwd,String newPwd) {
       if(StringUtils.isEmpty(oldPwd) || StringUtils.isEmpty(newPwd)){
           return error(local("user.empty.password"));
       }
       String userId = SecurityUtils.get().getId();
       CspUserInfo result = cspUserService.selectByPrimaryKey(userId);
       if(!MD5Utils.md5(oldPwd).equals(result.getPassword())){
           return error(local("user.error.old.password"));
       }
        result.setPassword(MD5Utils.md5(newPwd));
       cspUserService.updateByPrimaryKeySelective(result);
       return success();
    }


    /**
     * 发送绑定邮件
     */
    @RequestMapping("/toBind")
    public String toBind(String email) {
        if(StringUtils.isEmail(email)){
            return error(local("user.error.email.format"));
        }
        String userId = SecurityUtils.get().getId();
        try {
            cspUserService.sendMail(email,userId);
        } catch (SystemException e) {
            return error(e.getMessage());
        }
        return success();

    }

    /**
     * 绑定邮箱
     * @param code
     * @return
     * @throws SystemException
     */
    @RequestMapping("/bindEmail")
    public String bindEmail(String code) throws SystemException {
        String key = Constants.EMAIL_LINK_PREFIX_KEY + code;
        String result = redisCacheUtils.getCacheObject(key);
        if (result == null) {  //链接超时
            return "/test";
        } else {
            cspUserService.doBindMail(key, result);
            return "/register/bindOk";
        }

    }




    /**
     * 绑定手机号
     * @param mobile
     * @param captcha
     * @return
     */
    @RequestMapping("/bindMobile")
    public String bindMobile(String mobile,String captcha)  {
        if(StringUtils.isMobile(mobile) || StringUtils.isEmpty(captcha)){
            return error(local("error.param"));
        }
        try {
            cspUserService.checkCaptchaIsOrNotValid(captcha,mobile);
        } catch (SystemException e) {
            return error(e.getMessage());
        }
        String userId = SecurityUtils.get().getId();
        try {
            cspUserService.doBindMobile(mobile,captcha,userId);
        } catch (SystemException e) {
            return error(e.getMessage());
        }
        return success();
    }


    /**
     *
     * @param type 0代表邮箱，1代表手机
     * @return
     */
    @RequestMapping("/unbindEmailOrMobile")
    public String unbindEmailOrMobile(Integer type){

        String userId = SecurityUtils.get().getId();
        try {
            cspUserService.doUnbindEmailOrMobile(type,userId);
        } catch (SystemException e) {
            return error(e.getMessage());
        }
        return success();

    }



    /**
     * 绑定或解绑第三方账号
     * third_party_id 1代表微信，2代表微博，3代表facebook,4代表twitter,5代表YaYa医师
     * 解绑只传third_party_id，YaYa医师绑定传YaYa账号，密码,third_party_id
     */
    @RequestMapping("changeBindStatus")
    public String changeBindStatus(BindInfo info)  {

        String userId = SecurityUtils.get().getId();
        //第三方账号绑定操作
        if (!StringUtils.isEmpty(info.getUniqueId())) {
            try {
                cspUserService.doBindThirdAccount(info,userId);
            } catch (SystemException e) {
                return error(e.getMessage());
            }
        }else {
            //解绑操作
            try {
                cspUserService.doUnbindThirdAccount(info,userId);
            } catch (SystemException e) {
                return error(e.getMessage());
            }
        }
            return success();
    }


    /**
     * 手机号码 + 验证码登录
     *
     * @param mobile
     * @param captcha
     */
    protected void loginByMobile(String mobile, String captcha, HttpServletRequest request) {
        if (StringUtils.isEmpty(mobile)) {
            error(SpringUtils.getMessage("user.empty.mobile"));
        }
        if (StringUtils.isEmpty(captcha)) {
            error(SpringUtils.getMessage("user.empty.captcha"));
        }

        // 检查验证码是否有效

        boolean result = false;

            // 根据手机号码检查用户是否存在
            CspUserInfo userInfo = cspUserService.findByLoginName(mobile);
            if (userInfo == null) {
                error(SpringUtils.getMessage("user.error.nonentity"));
            }

            // 登录成功，返回用户信息
            loginSuccess(userInfo, userInfo.getToken(), request);




    }

    /**
     * 第三方登录 根据uniqueId检查是否有注册过csp账号，如果注册过，登录成功返回用户信息；
     * 反之，根据客户端传过来的第三方信息，保存到数据库，再登录返回用户信息
     * type 1代表微信,2代表微博,3代表facebook,4代表twitter 5代表YaYa
     */
    protected void loginByThirdParty(CspUserInfoDTO userDTO, HttpServletRequest request) {
        if (userDTO == null) {
            error(SpringUtils.getMessage("error.param"));
        }

        String uniqueId = userDTO.getUniqueId();
        // 检查用户是否存在
        CspUserInfo userInfo = cspUserService.findBindUserByUniqueId(uniqueId);

        // 用户不存在,则获取第三方用户信息 保存至CSP用户表及绑定用户表
        if (userInfo == null) {
            userInfo = cspUserService.saveThirdPartyUserInfo(userDTO);
        }

        // 缓存及更新用户登录时间
        loginSuccess(userInfo, userInfo.getToken(), request);


    }

    /**
     * 登录成功 返回用户信息
     *
     * @param userInfo
     */
    protected void loginSuccess(CspUserInfo userInfo, String token, HttpServletRequest request) {
        // 缓存用户信息
        cachePrincipal(userInfo, token);
        // 更新用户登录时间及ip
        userInfo.setLastLoginTime(new Date());
        userInfo.setLastLoginIp(request.getRemoteAddr());
        cspUserService.updateByPrimaryKey(userInfo);
        success(userInfo);
    }

    /**
     * 缓存用户信息
     *
     * @param user
     * @param token
     * @return
     */
    protected Principal cachePrincipal(CspUserInfo user, String token) {
        if (CheckUtils.isNotEmpty(token)) {
            token = UUIDUtil.getUUID();
            user.setToken(token);
            cspUserService.updateByPrimaryKey(user);
        }
        Principal principal = createPrincipal(user, token);
        redisCacheUtils.setCacheObject(Constants.TOKEN + "_" + token, principal, Constants.TOKEN_EXPIRE_TIME);
        return principal;
    }

    private Principal createPrincipal(CspUserInfo user, String token) {
        Principal principal = Principal.build(user);
        return principal;
    }

    private Principal getLegalToken(CspUserInfo user) {
        Principal principal = null;
        if (StringUtils.isNotEmpty(user.getToken())) {
            //清除token 踢出之前的认证信息
            disablePrincipal(user.getToken());
        }
        String token = UUIDUtil.getUUID();
        user.setToken(token);
        principal = cachePrincipal(user, token);
        return principal;
    }

    private void disablePrincipal(String token) {
        redisCacheUtils.delete(Constants.TOKEN + "_" + token);
    }


}