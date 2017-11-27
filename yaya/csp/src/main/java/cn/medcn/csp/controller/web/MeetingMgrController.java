package cn.medcn.csp.controller.web;

import cn.medcn.common.Constants;
import cn.medcn.common.ctrl.FilePath;
import cn.medcn.common.dto.FileUploadResult;
import cn.medcn.common.excptions.SystemException;
import cn.medcn.common.pagination.MyPage;
import cn.medcn.common.pagination.Pageable;
import cn.medcn.common.service.FileUploadService;
import cn.medcn.common.service.OfficeConvertProgress;
import cn.medcn.common.service.OpenOfficeService;
import cn.medcn.common.supports.FileTypeSuffix;
import cn.medcn.common.supports.upload.FileUploadProgress;
import cn.medcn.common.utils.*;
import cn.medcn.csp.controller.CspBaseController;
import cn.medcn.csp.dto.CspAudioCourseDTO;
import cn.medcn.csp.security.Principal;
import cn.medcn.csp.security.SecurityUtils;
import cn.medcn.meet.dto.CourseDeliveryDTO;
import cn.medcn.meet.model.*;
import cn.medcn.meet.service.AudioService;
import cn.medcn.meet.service.CourseCategoryService;
import cn.medcn.meet.service.LiveService;
import cn.medcn.user.model.AppUser;
import cn.medcn.user.model.UserFlux;
import cn.medcn.user.service.AppUserService;
import cn.medcn.user.service.UserFluxService;
import com.github.pagehelper.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.medcn.csp.CspConstants.MIN_FLUX_LIMIT;

/**
 * Created by lixuan on 2017/10/17.
 */
@Controller
@RequestMapping(value = "/mgr/meet")
public class MeetingMgrController extends CspBaseController {

    @Autowired
    protected AudioService audioService;

    @Value("${app.file.upload.base}")
    protected String fileUploadBase;

    @Value("${app.file.base}")
    protected String fileBase;

    @Autowired
    protected OpenOfficeService openOfficeService;

    @Autowired
    protected FileUploadService fileUploadService;

    @Autowired
    protected AppUserService appUserService;

    @Autowired
    protected CourseCategoryService courseCategoryService;

    @Autowired
    protected LiveService liveService;

    @Autowired
    protected UserFluxService userFluxService;

    /**
     * 查询当前用户的课件列表
     *
     * @param pageable
     * @param model
     * @return
     */
    @RequestMapping(value = "/list")
    public String list(Pageable pageable, Model model, String keyword, Integer playType, String sortType) {
        pageable.setPageSize(6);

        //打开了投稿箱的公众号列表
        Pageable pageable2 = new Pageable();
        MyPage<AppUser> myPage = appUserService.findAccepterList(pageable2);
        AppUser.splitUserAvatar(myPage.getDataList(), fileBase);
        model.addAttribute("accepterList", myPage.getDataList());
        //web获取当前用户信息
        Principal principal = getWebPrincipal();
        sortType = CheckUtils.isEmpty(sortType) ? "desc" : sortType;

        pageable.put("sortType", sortType);
        pageable.put("cspUserId", principal.getId());
        pageable.put("keyword", keyword);
        pageable.put("playType", playType);

        model.addAttribute("keyword", keyword);
        model.addAttribute("playType", playType);
        model.addAttribute("sortType", sortType);

        MyPage<CourseDeliveryDTO> page = audioService.findCspMeetingList(pageable);

        //如果第二页或其他页数查找到无会议时，用前一页的会议列表代替(不加以下判断，删除会议时可能会出现无会议内容的情况)
        if(page.getDataList().size() == 0 && pageable.getPageNum() != 1){
            pageable.setPageNum(pageable.getPageNum() - 1);
            page = audioService.findCspMeetingList(pageable);
        }

        CourseDeliveryDTO.splitCoverUrl(page.getDataList(),fileBase);
        model.addAttribute("page", page);

        return localeView("/meeting/list");
    }

    /**
     * 进入投屏界面
     *
     * @param courseId
     * @param model
     * @return
     */
    @RequestMapping(value = "/screen/{courseId}")
    public String screen(@PathVariable Integer courseId, Model model, HttpServletRequest request) throws SystemException {
        AudioCourse course = audioService.findAudioCourse(courseId);
        Principal principal = getWebPrincipal();
        if (!principal.getId().equals(course.getCspUserId())) {
            throw new SystemException(local("meeting.error.not_mine"));
        }

        if (course.getPlayType() == null) {
            course.setPlayType(AudioCourse.PlayType.normal.getType());
        }

        if (course.getPlayType().intValue() == AudioCourse.PlayType.normal.getType()) {
            AudioCoursePlay play = audioService.findPlayState(courseId);
            model.addAttribute("record", play);
        } else {
            //查询出直播信息
            Live live = liveService.findByCourseId(courseId);
            model.addAttribute("live", live);
        }

        model.addAttribute("course", course);
        String wsUrl = genWsUrl(request, courseId);
        model.addAttribute("wsUrl", wsUrl);

        String scanUrl = genScanUrl(request, courseId);
        //判断二维码是否存在 不存在则重新生成
        String qrCodePath = FilePath.QRCODE.path + "/course/" + courseId + ".png";
        boolean qrCodeExists = FileUtils.exists(fileUploadBase + qrCodePath);
        if (!qrCodeExists) {
            QRCodeUtils.createQRCode(scanUrl, fileUploadBase + qrCodePath);
        }

        model.addAttribute("fileBase", fileBase);
        model.addAttribute("qrCodeUrl", qrCodePath);

        return localeView("/meeting/screen");
    }


    /**
     * 生成二维码的地址
     *
     * @param request
     * @return
     */
    protected String genScanUrl(HttpServletRequest request, Integer courseId) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(request.getScheme());
        buffer.append("://").append(request.getServerName()).append(":").append(request.getServerPort());
        buffer.append("/api/meeting/scan/callback?courseId=");
        buffer.append(courseId);
        return buffer.toString();
    }

    /**
     * 进入课件编辑页面
     * 如果courseId为空 则查找最近编辑的未发布的AudioCourse
     *
     * @param courseId
     * @param model
     * @return
     */
    @RequestMapping(value = "/edit")
    public String edit(Integer courseId, Model model, HttpServletRequest request) {
        uploadClear(request);
        convertClear(request);

        Principal principal = getWebPrincipal();
        AudioCourse course = null;
        if (courseId != null) {
            course = audioService.findAudioCourse(courseId);
        } else {
            course = audioService.findLastDraft(principal.getId());
            if (course == null) {
                course = new AudioCourse();
                course.setPlayType(AudioCourse.PlayType.normal.getType());
                course.setPublished(false);
                course.setShared(false);
                course.setCspUserId(principal.getId());
                course.setTitle("");
                course.setCreateTime(new Date());
                course.setSourceType(AudioCourse.SourceType.csp.ordinal());
                audioService.insert(course);
            }
        }
        if (course.getPlayType() == null) {
            course.setPlayType(AudioCourse.PlayType.normal.getType());
        }

        if (course.getCategoryId() != null) {
            model.addAttribute("courseCategory", courseCategoryService.selectByPrimaryKey(course.getId()));
        }

        model.addAttribute("rootList", courseCategoryService.findByLevel(CourseCategory.CategoryDepth.root.depth));
        model.addAttribute("subList", courseCategoryService.findByLevel(CourseCategory.CategoryDepth.sub.depth));
        model.addAttribute("course", course);
        model.addAttribute("fileBase", fileBase);

        if (course.getPlayType() == null) {
            course.setPlayType(AudioCourse.PlayType.normal.getType());
        }

        if (course.getPlayType() > AudioCourse.PlayType.normal.getType()) {
            model.addAttribute("live", liveService.findByCourseId(course.getId()));
        }
        UserFlux flux = userFluxService.selectByPrimaryKey(principal.getId());
        float fluxValue = flux == null ? 0f : Math.round(flux.getFlux() * 1.0f / Constants.BYTE_UNIT_K * 100) * 1.0f / 100;
        model.addAttribute("flux", fluxValue);

        return localeView("/meeting/edit");
    }

    /**
     * 上传PPT或者PDF文件 并转换成图片
     *
     * @param file
     * @param courseId
     * @param request
     * @return
     */
    @RequestMapping(value = "/upload")
    @ResponseBody
    public String upload(@RequestParam(value = "file") MultipartFile file, Integer courseId, HttpServletRequest request) {
        String fileName = file.getOriginalFilename();
        FileUploadResult result;
        try {
            result = fileUploadService.upload(file, FilePath.TEMP.path);
        } catch (SystemException e) {
            return local("upload.error");
        }
        String imgDir = FilePath.COURSE.path + "/" + courseId + "/ppt/";
        List<String> imgList = null;
        if (result.getRelativePath().endsWith(".ppt") || result.getRelativePath().endsWith(".pptx")) {
            imgList = openOfficeService.convertPPT(fileUploadBase + result.getRelativePath(), imgDir, courseId, request);
        } else if (result.getRelativePath().endsWith(".pdf")) {
            imgList = openOfficeService.pdf2Images(fileUploadBase + result.getRelativePath(), imgDir, courseId, request);
        }
        if (CheckUtils.isEmpty(imgList)) {
            return error(local("upload.convert.error"));
        }
        AudioCourse course = audioService.selectByPrimaryKey(courseId);
        if (course != null) {
            course.setTitle(fileName.substring(0, fileName.lastIndexOf(".")));
            audioService.updateByPrimaryKey(course);
        }
        audioService.updateAllDetails(courseId, imgList);
        return success();
    }


    protected void handleHttpPath(AudioCourse course) {
        handleHttpUrl(fileBase, course);
    }

    /**
     * 进入到PPT明细编辑页面
     *
     * @param courseId
     * @param model
     * @return
     * @throws SystemException
     */
    @RequestMapping(value = "/details/{courseId}")
    public String details(@PathVariable Integer courseId, Model model) throws SystemException {
        AudioCourse course = audioService.findAudioCourse(courseId);
        if (course == null) {
            throw new SystemException(local("source.not.exists"));
        }
        handleHttpPath(course);
        Principal principal = getWebPrincipal();
        if (!principal.getId().equals(course.getCspUserId())) {
            throw new SystemException(local("meeting.error.not_mine"));
        }
        model.addAttribute("course", course);
        return localeView("/meeting/details");
    }

    /**
     * @param file
     * @param index
     * @return
     */
    @RequestMapping(value = "/detail/add")
    @ResponseBody
    public String add(@RequestParam(value = "file") MultipartFile file, Integer courseId, Integer index) {
        boolean isPicture = isPicture(file.getOriginalFilename());
        String dir = FilePath.COURSE.path + "/" + courseId + "/" + (isPicture ? "ppt" : "video");
        FileUploadResult result;
        try {
            result = fileUploadService.upload(file, dir);
        } catch (SystemException e) {
            e.printStackTrace();
            return error(e.getMessage());
        }
        AudioCourseDetail detail = new AudioCourseDetail();
        detail.setCourseId(courseId);
        detail.setSort(index + 1);

        if (isPicture) {
            detail.setImgUrl(result.getRelativePath());
        } else {
            detail.setVideoUrl(result.getRelativePath());
            detail.setImgUrl(dir + "/" + FFMpegUtils.printScreen(fileUploadBase + result.getRelativePath()));
        }

        audioService.addDetail(detail);
        return success();
    }


    protected boolean isPicture(String fileName) {
        fileName = fileName.toLowerCase();
        boolean isPic = fileName.endsWith(FileTypeSuffix.IMAGE_SUFFIX_JPG.suffix)
                || fileName.endsWith(FileTypeSuffix.IMAGE_SUFFIX_JPEG.suffix)
                || fileName.endsWith(FileTypeSuffix.IMAGE_SUFFIX_PNG.suffix);
        return isPic;
    }


    @RequestMapping(value = "/detail/del/{courseId}/{detailId}")
    public String del(@PathVariable Integer courseId, @PathVariable Integer detailId) {
        AudioCourseDetail detail = audioService.findDetail(detailId);
        Integer sort = 1;
        if (detail != null) {
            sort = detail.getSort();
            audioService.deleteDetail(detail.getCourseId(), detailId);
        }
        List<AudioCourseDetail> details = audioService.findDetails(detail.getCourseId());
        sort--;
        if (sort == details.size()) {
            sort--;
        }
        return "redirect:/mgr/meet/details/" + courseId + "?index=" + sort;
    }

    @RequestMapping(value = "/del/{courseId}")
    @ResponseBody
    public String del(@PathVariable Integer courseId) {
        AudioCourse course = audioService.selectByPrimaryKey(courseId);
        Principal principal = getWebPrincipal();
        if (!principal.getId().equals(course.getCspUserId())) {
            return error(local("meeting.error.not_mine"));
        }
        audioService.deleteAudioCourse(courseId);
        return success();
    }


    @RequestMapping(value = "/more/{courseId}")
    public String more(@PathVariable Integer courseId, Model model) {
        model.addAttribute("courseId", courseId);
        return localeView("/meeting/more");
    }


    @RequestMapping(value = "/view/{courseId}")
    @ResponseBody
    public String view(@PathVariable Integer courseId, Model model) {
        AudioCourse course = audioService.findAudioCourse(courseId);
        handleHttpPath(course);
        return success(course);
    }


    @RequestMapping(value = "/copy/{courseId}")
    @ResponseBody
    public String copy(@PathVariable Integer courseId, String title) {
        AudioCourse course = audioService.selectByPrimaryKey(courseId);
        Principal principal = getWebPrincipal();
        if (!principal.getId().equals(course.getCspUserId())) {
            return error(local("meeting.error.not_mine"));
        }
        audioService.addCourseCopy(courseId, title);
        return success();
    }


    @RequestMapping(value = "/share/{courseId}")
    @ResponseBody
    public String share(@PathVariable Integer courseId, HttpServletRequest request) {
        String local = LocalUtils.getLocalStr();
        Principal principal = SecurityUtils.get();
        boolean abroad = principal.getAbroad();
        StringBuffer buffer = new StringBuffer();
        buffer.append("id=").append(courseId).append("&").append(Constants.LOCAL_KEY).append("=")
                .append(local).append("&abroad=" + (abroad ? 1 : 0));
        String signature = DESUtils.encode(Constants.DES_PRIVATE_KEY, buffer.toString());

        StringBuffer buffer2 = new StringBuffer();
        try {
            buffer2.append(request.getScheme()).append("://").append(request.getServerName()).append(":")
                    .append(request.getServerPort()).append(request.getContextPath()).append("/api/meeting/share?signature=")
                    .append(URLEncoder.encode(signature, Constants.CHARSET));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("shareUrl", buffer2.toString());
        return success(result);
    }


    @RequestMapping(value = "/save", method = RequestMethod.POST)
    public String save(CspAudioCourseDTO course, Integer openLive, String liveTime, RedirectAttributes redirectAttributes) throws SystemException {
        AudioCourse ac = course.getCourse();
        if (openLive != null && openLive == 1) {
            ac.setPlayType(AudioCourse.PlayType.live_video.getType()); //视频直播
            //判断是否有足够的流量
            UserFlux flux = userFluxService.selectByPrimaryKey(getWebPrincipal().getId());
            if (flux == null || flux.getFlux() < MIN_FLUX_LIMIT * Constants.BYTE_UNIT_K) {
                throw new SystemException(local("user.flux.not.enough"));
            }
        }

        if (ac.getPlayType() != null && ac.getPlayType().intValue() > 0) {// 直播的情况下
            audioService.updateAudioCourseInfo(ac, course.getLive());
        } else {
            audioService.updateAudioCourseInfo(ac, new AudioCoursePlay());
        }
        addFlashMessage(redirectAttributes, local("operate.success"));
        return "redirect:/mgr/meet/list";
    }


    @RequestMapping(value = "/upload/progress")
    @ResponseBody
    public String uploadProgress(HttpServletRequest request){
        FileUploadProgress progress = (FileUploadProgress) request.getSession().getAttribute(Constants.UPLOAD_PROGRESS_KEY);
        if(progress == null){
            progress = new FileUploadProgress();
        }
        return success(progress);
    }


    @RequestMapping(value = "/upload/clear")
    @ResponseBody
    public String uploadClear(HttpServletRequest request){
        request.getSession().removeAttribute(Constants.UPLOAD_PROGRESS_KEY);
        return success();
    }


    @RequestMapping(value = "/convert/progress")
    @ResponseBody
    public String convertProgress(HttpServletRequest request){
        OfficeConvertProgress progress = (OfficeConvertProgress) request.getSession().getAttribute(Constants.OFFICE_CONVERT_PROGRESS);
        if (progress == null) {
            progress = new OfficeConvertProgress(0, 0, 0);
        }
        return success(progress);
    }


    @RequestMapping(value = "/convert/clear")
    @ResponseBody
    public String convertClear(HttpServletRequest request){
        request.getSession().removeAttribute(Constants.OFFICE_CONVERT_PROGRESS);
        return success();
    }
}
