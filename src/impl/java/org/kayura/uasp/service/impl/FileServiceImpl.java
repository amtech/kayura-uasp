/**
 * Copyright 2015-2015 the original author or authors.
 * HomePage: http://www.kayura.org
 */
package org.kayura.uasp.service.impl;

import org.kayura.type.GeneralResult;
import org.kayura.type.Result;
import org.kayura.uasp.dao.FileMapper;
import org.kayura.uasp.po.FileInfo;
import org.kayura.uasp.po.FileRelation;
import org.kayura.uasp.service.FileService;
import org.kayura.uasp.vo.FileDownload;
import org.kayura.uasp.vo.FileUpload;
import org.kayura.utils.DateUtils;
import org.kayura.utils.KeyUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author liangxia@live.com
 */
public class FileServiceImpl implements FileService {

	@Autowired
	private FileMapper fileMapper;

	@Override
	public GeneralResult upload(FileUpload fu) {

		FileRelation fr = new FileRelation();
		fr.setFrId(KeyUtils.newId());
		fr.setTenantId(fu.getTenantId());
		fr.setBizId(fu.getBizId());
		fr.setCategory(fu.getCategory());
		fr.setUploaderId(fu.getUploaderId());
		fr.setUploaderName(fu.getUploaderName());
		fr.setUploadTime(DateUtils.now());
		fr.setAllowChange(fu.getAllowChange());
		fr.setSerial(fu.getSerial());
		fr.setTags(fu.getTags());

		// 在不允许修改文件内容时,可引用相同文件,以减少磁盘存储.
		String fileId = null;
		Boolean isNewFile = false;
		if (!fu.getAllowChange()) {
			fileId = fileMapper.getKeyForFileInfo(fu.getMd5());
		}

		// 若没有相同的文件内容,将创建新文件.
		if (fileId == null) {
			fileId = KeyUtils.newId();

			FileInfo fi = new FileInfo();
			fi.setFileId(fileId);
			fi.setContentType(fu.getContentType());
			fi.setFileSize(fu.getFileSize());
			fi.setPostfix(fu.getPostfix());
			fi.setLogicPath(fu.getLogicPath());
			fi.setMd5(fu.getMd5());
			fi.setIsEncrypted(fu.getIsEncrypt());
			fi.setSalt(fu.getSalt());
			fi.setStatus(FileInfo.STATUS_TEMP);

			// 将文件信息添加至数据库.
			fileMapper.insertFileInfo(fi);

			isNewFile = true;
		}

		// 记录文件信息Id, 将文件关联保存至数据库.
		fr.setFileId(fileId);
		fileMapper.insertFileRelation(fr);

		// 创建返回值对象.
		GeneralResult r = new GeneralResult();
		r.add("frid", fr.getFrId());
		r.add("fileid", fr.getFileId());
		r.add("newfile", isNewFile);

		return r;
	}

	@Override
	public Result<FileDownload> download(String frId) {

		Result<FileDownload> r = new Result<FileDownload>();

		FileRelation fr = fileMapper.getFileRelationById(frId);
		if (fr == null) {
			r.setError("frId: %s not exists。", frId);
			return r;
		}

		FileInfo fi = fileMapper.getFileInfoById(fr.getFileId());
		if (fi == null) {
			r.setError("fileId: %s not exists.", fr.getFileId());
			return r;
		}

		FileDownload fd = new FileDownload();
		fd.setFrId(fr.getFrId());
		fd.setLogicPath(fi.getLogicPath());
		fd.setFileId(fr.getFileId());
		fd.setFileName(fr.getFileName());
		fd.setContentType(fi.getContentType());
		fd.setIsEncrypted(fi.getIsEncrypted());
		fd.setSalt(fd.getSalt());		

		r.setSuccess("读取下载文件信息成功.");
		r.setData(fd);

		return r;
	}

}
