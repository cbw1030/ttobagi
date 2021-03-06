package com.ttobagi.web.controller.user;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.Principal;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.websocket.server.PathParam;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.mysql.cj.Session;
import com.ttobagi.web.entity.Community;
import com.ttobagi.web.entity.CommunityCategory;
import com.ttobagi.web.entity.CommunityComment;
import com.ttobagi.web.entity.CommunityFiles;
import com.ttobagi.web.entity.CommunityView;
import com.ttobagi.web.entity.Member;
import com.ttobagi.web.service.CommunityCategoryService;
import com.ttobagi.web.service.CommunityCommentService;
import com.ttobagi.web.service.CommunityService;

@Controller
@RequestMapping("/user/community/")
public class CommunityController {
	
	@Autowired
	CommunityService service;
	@Autowired
	CommunityCommentService commentService;
	@Autowired
	CommunityCategoryService categoryService;
	
	@RequestMapping("index")
	public String index(Model model) {
		
		List<CommunityCategory> categoryType = categoryService.getCateList();
		
		model.addAttribute("categorytype", categoryType);
		
		return "user.community.index";
	}
	
	//list
	@GetMapping("{type}")
	public String list(
			Model model, 
			@PathVariable("type") String type) {
		List<CommunityView> bestList = service.getViewList(0, 5, type, "hit");
		List<CommunityView> list = service.getViewList(0, 20, type, "regDate");
		
		CommunityCategory category = categoryService.getCategory(type);
		
		model.addAttribute("bestList", bestList);
		model.addAttribute("list", list);
		model.addAttribute("cate", category);
		
		return "user.community."+type+".list";
	}
	
	//detail
	@GetMapping("{type}/{communityId}")
	public String detail(
			Model model, 
			@PathVariable("type") String type, 
			@PathVariable("communityId") int communityId) {
		
		//조회수 업데이트
		Community community = service.get(communityId);
		community.setHit(community.getHit()+1);		
		service.update(community);
		
		CommunityCategory category = categoryService.getCategory(type);
		CommunityView communityView = service.getView(communityId);
		List<CommunityComment> comments = commentService.commentList(communityId);
		
		model.addAttribute("comment",comments);
		model.addAttribute("cate", category);
		model.addAttribute("d", communityView);

		return "user.community."+type+".detail";
	}

	@GetMapping("{type}/{communityId}/edit")
	public String edit(
			Model model, 
			@PathVariable("type") String type, 
			@PathVariable("communityId") int communityId) {
		CommunityView list = service.getView(communityId);
		CommunityFiles files = service.getFiles(communityId);
		CommunityCategory category = categoryService.getCategory(type);
		
		model.addAttribute("cate", category);
		model.addAttribute("e", list);
		
		return "user.community."+type+".edit";
	}
	
	@PostMapping("{type}/{communityId}/edit")
	public String edit(
			Community community,
			//파일
			HttpServletRequest request,
			CommunityFiles communityFiles,
			@PathVariable("communityId") int communityId,
			@PathVariable("type") String type,
			@RequestParam("file") MultipartFile file) throws IllegalStateException, IOException {
		
		String title = community.getTitle();
		String content = community.getContent();
		
		String fileName = file.getOriginalFilename();
		//파일을 등록했을 때만 실행
		if( fileName != null && !fileName.equals("")) {
			String url = "resources/static/images/user/community/"+type+"/"+communityId;
			String realPath = request.getServletContext().getRealPath(url);
			
			File realPathFile = new File(realPath);
			if( !realPathFile.exists())
				realPathFile.mkdirs();
			
			String uploadedFilePath = realPath + File.separator + fileName;		
			File uploadedFile = new File(uploadedFilePath);
			
			file.transferTo(uploadedFile);
			
			//객체에 파일이름이랑 id set
			communityFiles.setName(fileName);
			communityFiles.setCommunityId(communityId);
			
			//그냥 등록이라면 insert
			if(service.getFiles(communityId) == null) {
				service.insertFiles(communityFiles);
			}
			//파일이 있다면 update
			else { 
				service.updateFiles(communityFiles);			
			}
		}			
		
		//파일 외 텍스트나 제목 업데이트
		Community origin = service.get(communityId);
		origin.setTitle(title);
		origin.setContent(content);
		
		service.update(origin);

		return "redirect:../"+communityId;
	}
	
	@GetMapping("{type}/reg")
	public String reg(Model model, @PathVariable("type") String type) {
		CommunityCategory category = categoryService.getCategory(type);
		
		model.addAttribute("cate", category);
		model.addAttribute("type", type);

		return "user.community."+type+".reg";
	}
	
	@PostMapping("{type}/reg")
	public String reg(
			@PathVariable("type") String type,
			Community community,
			CommunityFiles communityFiles,
			Member member,
			@RequestParam("file") MultipartFile file,
			HttpServletRequest request,
			HttpSession session) throws IllegalStateException, IOException{
		
		int id = (int) session.getAttribute("id");
		String fileName = file.getOriginalFilename();

		CommunityCategory category = categoryService.getCategory(type);
		int categoryId = category.getId();

		community.setMemberId(id);
		community.setCategoryId(categoryId);
		
		service.insert(community);
		
		int lastNum = service.getLastNum();
		//파일을 등록했을 때만 실행
		if( fileName != null && !fileName.equals("")) {
			String url = "resources/static/images/user/community/"+type+"/"+lastNum;
			String realPath = request.getServletContext().getRealPath(url);
			
			File realPathFile = new File(realPath);
			if( !realPathFile.exists())
				realPathFile.mkdirs();
			
			String uploadedFilePath = realPath + File.separator + fileName;		
			File uploadedFile = new File(uploadedFilePath);
			
			file.transferTo(uploadedFile);
			
			//객체에 파일이름이랑 id set
			communityFiles.setName(fileName);
			communityFiles.setCommunityId(lastNum);
			
			service.insertFiles(communityFiles);			
		}
		
		return "redirect:../"+type;
	}
	
	@GetMapping("{type}/{communityId}/del")
	public String delete(
			@PathVariable("type") String type, 
			@PathVariable("communityId") int communityId
		) {
		commentService.deleteCommentAll(communityId);
		service.deleteFiles(communityId);
		service.delete(communityId);
		
		return "redirect:../../"+type;
	}
}
