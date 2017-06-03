package com.ycg.exteriorline.web.action.system;

import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.ja.core.util.CommomUtil;
import com.ja.core.util.DateUtil;
import com.ja.core.util.PropertiesUtil;
import com.ycg.exteriorline.model.DriverContract;
import com.ycg.exteriorline.model.DriverInfMaintain;
import com.ycg.exteriorline.model.VmVehicleInfo;
import com.ycg.exteriorline.model.dto.DriverFinancialDto;
import com.ycg.exteriorline.model.dto.FinancialDto;
import com.ycg.exteriorline.model.em.FinancialEnum;
import com.ycg.exteriorline.service.CodeGeneratorService;
import com.ycg.exteriorline.service.DriverContractService;
import com.ycg.exteriorline.service.DriverInfMaintainService;
import com.ycg.exteriorline.service.VmVehicleInfoService;
import com.ycg.exteriorline.web.action.util.BaseAction;
import com.ycg.exteriorline.web.action.util.UpLoad;

/**
 * 司机金融服务Action<br>
 * @author huangyz
 * @version [V1.0, 2017-4-11]
 */
@Controller
@RequestMapping(value = "/financial/", produces = { "application/json;charset=UTF-8" })
public class DriverFinancialAction extends BaseAction {

    private Log log = LogFactory.getLog(this.getClass());

    @Autowired
    private DriverInfMaintainService driverInfMaintainService;

    @Autowired
    private DriverContractService driverContractService;
    
    @Autowired
    private CodeGeneratorService codeGeneratorService;

    @Autowired
    private VmVehicleInfoService vmVehicleInfoService;

    /**
     * 功能描述: <br>
     * 更新司机驾驶证信息（金融服务所需）
     * @version [V1.0, 2017-4-11]
     * @param request
     * @return
     */
    @RequestMapping(value = "getLicenseInfo", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> getLicenseInfo(HttpServletRequest request) {
        Map<String, Object> map = this.checkLoginInfo(request);
        Boolean flag = (Boolean) map.get("success");
        if (!flag);
        else {
            String sysCode = (String) map.get("sysCode");
            FinancialDto financialDto = driverInfMaintainService.getLicenseInfo(sysCode);

            map.put("result", (Serializable) financialDto);
        }
        map.put("sysCode", null);

        return map;
    }

    /**
     * 功能描述: <br>
     * 更新司机金融认证状态
     * @version [V1.0, 2017-4-11]
     * @param request
     * @return
     */
    @RequestMapping(value = "updateLicenseInfo", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> updateLicenseInfo(HttpServletRequest request, String licenseStart, String licenseEnd, String licenseType,
            String initialProofTime) {
        Map<String, Object> map = this.checkLoginInfo(request);
        log.info("需要司机金融认证的信息 : 司机sysCode : " + map.get("success") + ", licenseStart:" + licenseStart + ", licenseEnd : " + licenseEnd
                + ",licenseType : " + licenseType + ", initialProofTime" + initialProofTime);
        Boolean flag = (Boolean) map.get("success");
        if (!flag);
        else {
            String sysCode = (String) map.get("sysCode");
            flag = driverInfMaintainService.updateLicenseInfo(sysCode, licenseStart, licenseEnd, licenseType, initialProofTime);
            map.put("result", flag);
        }
        map.put("sysCode", null);

        return map;
    }

    /**
     * 功能描述: <br>
     * 获取司机金融认证状态
     * @version [V1.0, 2017-4-12]
     * @param request : 请求
     * @return : 返回结果
     */
    @RequestMapping(value = "getDriverFinancialStatus", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> getDriverFinancialStatus(HttpServletRequest request) {
        Map<String, Object> map = this.checkLoginInfo(request);
        Map<String, Object> resultMap = new HashMap<String, Object>();
        Boolean flag = (Boolean) map.get("success");
        if (!flag) ;
        else {
            String sysCode = (String) map.get("sysCode");

            DriverInfMaintain drInfMaintain = driverInfMaintainService.getDriverInfo(sysCode);

            DriverContract driverContract = driverContractService.getContractInfo(drInfMaintain.getDriverIdCardNumber());

            String status = this.getStatus(drInfMaintain, driverContract);
            // 查看额度 存在额度返回发达大页面地址
            if ("3".equals(status)) {
                resultMap.put("reason", drInfMaintain.getAuditFinancialNote());
            } else if ("5".equals(status)) {
                String url = driverContract.getAontractCheckUrl() == null ? driverContract.getFddViewIndex() : driverContract
                        .getAontractCheckUrl();
                resultMap.put("pageAddress", url);
            } else if ("6".equals(status)) {
                resultMap.put("reason", "您的额度正在批复中, 请耐心等待");
            }

            resultMap.put("status", status);

            map.put("result", resultMap);
        }
        map.put("sysCode", null);

        return map;
    }

    /**
     * 功能描述: <br>
     * 对认证状态进行封装
     * @version [V1.0, 2017-4-11]
     * @param drInfMaintain : 司机信息
     * @return :　返回结果
     * @throws IOException
     */
    private String getStatus(DriverInfMaintain drInfMaintain, DriverContract driverContract) {
        String status = "0";
        String financialStatus = drInfMaintain.getFinancialStatus();
        String aontractStatus = null;
        Map<String, Object> params = new HashMap<String, Object>();
        if (null != driverContract) {
            aontractStatus = driverContract.getAontractStatus();
        }

        if ("2".equals(financialStatus)) {
            if ("1".equals(aontractStatus) && "1".equals(drInfMaintain.getLimitStatus())) {
                Properties properties = null;
                try {
                    properties = PropertiesUtil.loadPropertiesFromSrc("apk.properties");
                    params.put("orgNo", properties.getProperty("fdd_aontract_code"));
                    params.put("driverName", drInfMaintain.getDriverName());
                    params.put("cardNo", drInfMaintain.getDriverIdCardNumber());
                    params.put("driverPhone", drInfMaintain.getDriverPhone());
                    Map<String, Object> map = new HashMap<String, Object>();
                    if (!drInfMaintain.getAontractStatus().equals(CommomUtil.DRIVER_CONTRACT_YES)) {
                        map = driverContractService.fingDriverContract(params, properties.getProperty("fdd_aontract_query"));
                        boolean flag = (Boolean) map.get("flag");
                        status = flag == true ? "5" : "4";
                    } else {
                        status = "5";
                    }
                } catch (IOException ex) {
                    log.error("服务器异常 : ", ex);
                    status = "4";
                }
            } else if ("0".equals(drInfMaintain.getLimitStatus()) && !"1".equals(aontractStatus)) {
                status = "6";
            } else {
                status = "4";
            }
        } else {
            status = drInfMaintain.getFinancialStatus();
        }

        return status;
    }

    /**
     * 功能描述: <br>
     * 申请法大大协议
     * @version [V1.0, 2017-4-12]
     * @param request : 请求
     * @return : 返回结果
     */
    @RequestMapping(value = "applyFddAontract", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> applyFddAontract(HttpServletRequest request) {
        Map<String, Object> map = this.checkLoginInfo(request);
        Boolean flag = (Boolean) map.get("success");
        Map<String, Object> restultMap = new HashMap<String, Object>();

        if (!flag) ;
        else {
            try {

                Properties properties = PropertiesUtil.loadPropertiesFromSrc("apk.properties");
                String sysCode = (String) map.get("sysCode");
                DriverInfMaintain drInfMaintain = driverInfMaintainService.getDriverInfo(sysCode);
                // 判断是否有额度
                if ("0".equals(drInfMaintain.getLimitStatus())) {
                    map.put("success", false);
                    map.put("msg", "您额度不够，不能开通服务");
                } else {
                    DriverContract driverContract = driverContractService.getContractInfo(drInfMaintain.getDriverIdCardNumber());

                    if (null != driverContract && CommomUtil.DRIVER_CONTRACT_YES.equals(driverContract.getAontractStatus())) {
                        String url = driverContract.getAontractCheckUrl() == null ? driverContract.getFddViewIndex() : driverContract
                                .getAontractCheckUrl();
                        restultMap.put("pageAddress", url);
                        // 已经申请成功状态直接跳转 法大大url
                        map.put("success", true);
                        map.put("msg", "操作成功");
                        map.put("status", "5");
                        map.put("result", restultMap);
                    } else { // 失败或者 待申请
                        Map<String, Object> params = new HashMap<String, Object>();
                        if (null == driverContract) {
                            // 生成合同id 一个司机对应一个合同 已存在合同不在生成
                            DriverContract dat = new DriverContract();

                            dat.setCarrierSysCode(properties.getProperty("fdd_aontract_code"));
                            dat.setCreatedTime(new Date());
                            dat.setDriverId(String.valueOf(drInfMaintain.getId()));
                            dat.setDriverPhone(drInfMaintain.getDriverPhone());
                            dat.setDriverIdcard(drInfMaintain.getDriverIdCardNumber());
                            dat.setDriverName(drInfMaintain.getDriverName());
                            driverContract = driverContractService.save(dat);
                        }

                        Map<String, Object> maps = new HashMap<String, Object>();
                        maps.put("driverPhone", drInfMaintain.getDriverPhone());
                        maps.put("cardNo", driverContract.getDriverIdcard());
                        maps.put("driverName", drInfMaintain.getDriverName());
                        maps.put("orgNo", driverContract.getCarrierSysCode());

                        flag = false;
                        Map<String, Object> mapId = driverContractService.fingDriverContract(maps,
                                properties.getProperty("fdd_aontract_query"));
                        flag = (Boolean) mapId.get("flag");
                        if (!flag && null != mapId.get("applyContractRelId")) {

                            params.put("applyContractRelId", mapId.get("applyContractRelId"));
                            params.put("driverPhone", drInfMaintain.getDriverPhone());
                            params.put("cardNo", driverContract.getDriverIdcard());
                            params.put("driverName", drInfMaintain.getDriverName());
                            params.put("orgNo", driverContract.getCarrierSysCode());
                            params.put("busiTime", DateUtil.getDateTime());
                            params.put("busiSeqNo", "sign" + DateUtil.getDateDay() + codeGeneratorService.createFddAontract());
                            // 调用法大大合同接口 填写需要参数
                            Map<String, Object> restMap = driverContractService.createDriverContract(params,
                                    properties.getProperty("fdd_aontract_index"));
                            flag = (Boolean) restMap.get("flag");
                            if (flag) {
                                driverContract.setFddViewIndex((String) restMap.get("message"));
                            }
                        }

                        map.put("success", true);
                        Map<String, Object> restMap = new HashMap<String, Object>();
                        restMap.put("pageAddress", driverContract.getFddViewIndex());
                        map.put("result", restMap);
                    }
                }

                map.put("sysCode", null);
            } catch (Exception exception) {
                log.error("签署法大大合同异常 : ", exception);
                map.put("success", false);
                map.put("msg", "操作失败");
            }
        }

        return map;
    }

    /**
     * 功能描述: <br>
     * 更新金融服务信息
     * @version [V1.0, 2017-5-26]
     * @param request : 请求
     * @return : 返回更新结果信息
     */
    @RequestMapping(value = "updateFinalcialInfo", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> getFinalcialServiceInfo(HttpServletRequest request, HttpServletResponse response,
            DriverFinancialDto driFinancialDto) {
        // 查询认证图片信息
        Map<String, Object> map = this.checkLoginInfo(request);
        Boolean flag = (Boolean) map.get("success");
        if (!flag) ;
        else {
            
            String sysCode = (String) map.get("sysCode");
            //司机信息
            DriverInfMaintain drInfMaintain = driverInfMaintainService.getDriverInfo(sysCode);
            // 车辆信息
            VmVehicleInfo vmVehicleInfo = vmVehicleInfoService.findSingleVmVehicleInfoBySyscode(sysCode);
            try {
                
                //司机信息
                drInfMaintain.setDriverName(driFinancialDto.getDriverName());
                drInfMaintain.setDriverPhone(driFinancialDto.getDriverPhone());
                drInfMaintain.setDriverDrivingLicense(driFinancialDto.getDriverDrivingLicense());
                drInfMaintain.setDriverCatchBeginLincence(DateUtil.str2Date(driFinancialDto.getDriverCatchBeginLincence()));
                drInfMaintain.setDriverLicenseValidateStrat(DateUtil.str2Date(driFinancialDto.getDriverLicenseValidateStrat()));
                drInfMaintain.setDriverLicenseValidateEnd(DateUtil.str2Date(driFinancialDto.getDriverLicenseValidateEnd()));
                drInfMaintain.setBankNo(driFinancialDto.getBankNo());
                drInfMaintain.setOpenAccount(driFinancialDto.getOpenAccount());
                
                //车辆信息 
                vmVehicleInfo.setVehicleId(driFinancialDto.getVehicleId());
                vmVehicleInfo.setDrivingLicenceNumber(driFinancialDto.getDrivingLicenceNumber());
                vmVehicleInfo.setDrivingLicenceTime(DateUtil.str2Date(driFinancialDto.getDrivingLicenceTime()));
                //异步上传图片
                FutureTask<Map<String, Map<String, Boolean>>> imgUploadFutureTask = UpLoad.upLoadAsyn(request, response, CommomUtil.FILE_TYPE_IMG);
                Map<String, Map<String, Boolean>> mapUrl = imgUploadFutureTask.get();
                boolean result = driverInfMaintainService.saveOrEdit(drInfMaintain, mapUrl, vmVehicleInfo);
                map.put("result", result);
            } catch (InterruptedException ex) {
               log.error("异常信息: ",ex);
            } catch (ExecutionException ex) {
                log.error("异常信息: ",ex);
            } catch (ParseException ex) {
                log.error("异常信息: ",ex);
            }
        }
        
        map.put("sysCode", null);

        return map;
    }
    
    /**
     * 
     * 功能描述: <br>
     * 查询 司机开通金融服务信息
     *
     * @version [V1.0, 2017-6-1]
     * @param request
     * @return
     */
    @RequestMapping(value = "getFinalcialInfo", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> getFinalcialServiceResult(HttpServletRequest request) {
        Map<String, Object> map = this.checkLoginInfo(request);
        Boolean flag = (Boolean) map.get("success");
        if (!flag) ;
        else {
            String sysCode = (String) map.get("sysCode");
        
            Map<String, Object> resMap = driverInfMaintainService.getDriFinancialInfo(sysCode);
            map.put("result", resMap.get("result"));
            map.put("status", resMap.get("status"));
            map.put("reason", resMap.get("reason") == null ? "" : resMap.get("reason"));
        }
        
        map.put("sysCode", null);

        return map;
    }
  
    /**
     * 
     * 功能描述: <br>
     * 测试s上传图片
     *
     * @version [V1.0, 2017-6-2]
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value="uploadImg", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> uploadImg(HttpServletRequest request, HttpServletResponse response, String bankCard) {
        Map<String, Object> map = new HashMap<String, Object>();
        try {
            String bankNo = (String)request.getParameter("bankCard");
            FutureTask<Map<String, Map<String, Boolean>>> imgUploadFutureTask = UpLoad.upLoadAsyn(request, response, CommomUtil.FILE_TYPE_IMG);
            Map<String, Map<String, Boolean>> mapUrl = imgUploadFutureTask.get();
            Map<String, Boolean> drivingLicence = mapUrl.get("File_0");
            Iterator<String> drivingLicenceIte = null;
            if (null != drivingLicence && !drivingLicence.isEmpty()) {
                drivingLicenceIte = drivingLicence.keySet().iterator();
            }
            map.put("success", true);
            map.put("msg", "上传成功");
            map.put("result", drivingLicenceIte);
        } catch (InterruptedException ex) {
            log.error("异常信息 : " + ex);
        } catch (ExecutionException ex) {
            log.error("异常信息 : " + ex);
        }
        
        return map;
        
    }

}
