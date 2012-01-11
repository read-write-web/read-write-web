// JavaScript Document


$(window).ready(function(){
						 
						 
						 /*
						 
						 // AJAX FILE UPLOADER
	var button1 = $('#status_upload_file'), interval;
		
		new AjaxUpload(button1, {
			onSubmit : function(file , ext){
				   
               	 if (! (ext && /^(jpg)$/i.test(ext))){
                        // extension is not allowed
                        alert('Error: invalid file extension (only jpg for the moment)');
                        // cancel upload
                        return false;
               	 }
       		 },
			action: 'LINKTOFILE.php', 
			name: 'status_file',
			onSubmit : function(file, ext){ 
				
				// Disable the send status button until it is sent
				$('#status_btn_submit').attr('disabled', 'disabled');
				
				// change button text, when user selects file			
				button1.text('Uploading');
								
				// If you want to allow uploading only 1 file at time,
				// you can disable upload button
				this.disable();
				
				// Uploding -> Uploading. -> Uploading...
				interval = window.setInterval(function(){
					var text = button1.text();
					if (text.length < 13){
						button1.text(text + '.');					
					} else {
						button1.text('Uploading');				
					}
				}, 200);
			},
			onComplete: function(file, response){ 
				button1.text('Uploaded');
							
				window.clearInterval(interval);
							
				// enable upload button
				this.enable();
				
				//Re enable the button to submit the post
				$('#status_btn_submit').removeAttr('disabled');
				
				// add file to the list
				$('#status_file_name').html(response);
				$('#status_file_name').fadeIn('fast');

				$('#status_file_temp_name').val(response);

				
				
			}
		});
		
		*/

});

$(document).ready(function(){





	// Column minimizer
	$('#min_right_column').click(function(){
										  
		$('#right_column').animate({width: 'toggle'});
		
		if($(this).hasClass('min_right_column_normal')){
			// for the button
			$(this).removeClass('min_right_column_normal');
			$(this).addClass('min_right_column_reverse');
			
			// for the left column
			$('#left_column').addClass('left_column_full');
			$('.text').addClass('text_full');
			$('.story_right_column').addClass('text_full');
			
		} else {
			// for the button
			$(this).removeClass('min_right_column_reverse');
			$(this).addClass('min_right_column_normal');	
			
			// for the left column
			$('#left_column').removeClass('left_column_full');
			$('.text').removeClass('text_full');
			$('.story_right_column').removeClass('text_full');
		}
	});
	





	var searching_results = new Array();
			
	$("#status_input").autocomplete( searching_results , {
		multiple: true,
		multipleSeparator: " ",
		scroll: true
	});	

	$("#main_search").autocomplete( searching_results , {
		multiple: true,
		multipleSeparator: " ",
		scroll: true
	});		
	

	$('#status_input').focus(function(){ 
		if($(this).val() === ''){
			$(this).animate({
					height: '70'
				}, 800, function() {
					// Animation complete.
			});
		}	
		$('#status_options_file').slideToggle('fast');
	});
	
	$('#status_input').blur(function(){ 
		/*if($(this).val() === ''){
			$(this).animate({
					height: '20'
				}, 800, function() {
					// Animation complete.
			});
		}
		$('#status_options_file').slideToggle('fast');
		*/
	});	
	

	$('.story_comment_textarea').live('focus',function(){ 
		if($(this).val() === ''){
			$(this).animate({
					height: '50'
				}, 800, function() {
					// Animation complete.
			});
		}							  
	});
	
	$('.story_comment_textarea').live('blur',function(){ 
		if($(this).val() === ''){
			$(this).animate({
					height: '20'
				}, 800, function() {
					// Animation complete.
			});
		}
	});	
	

	$('.show_comment').live('click',function(){
		var feed = $(this).attr('title');
		$('#comment_area_'+feed).slideToggle('fast');
		
		return false;
	});


	$('#logo').click(function(){
		
		window.location.href = "";
		
		return false;
	});




	
	
	
	$('#form_status').submit(function(){
		
		$.ajax({
			type: 'GET',
			url: 'LINKTOFILE.php',
			data: $(this).serialize(),
			dataType : 'json',
			beforeSend:function(){
				//$('#update span').html(befpreMsg);
				$('#status_input').blur();
			},
			success:function(data){
				
				
				
				if( data.error === false){
					//Success
		
					$('#listing').prepend(data.toprint);
					// Check if it is full screen
					if( $('#left_column').hasClass('left_column_full') ){
						$('.text').addClass('text_full');
						$('.story_right_column').addClass('text_full');	
					}
				
					$('#status_input').val('');
					$('#status_file_name').html('');
					$('#status_file_temp_name').val('');
		
					$('#status_input').focus();
					
					
				}
				if(data.error === true){
					//$('#update span').html(errorMsg);
					//results = false;
					alert("Message has not been added");
				}
							
			},
			error:function(data){
				//$('#update span').html(errorMsg);
				//results = false;
			}
		});	

		return false
	});
	
	
	
	
	
	
	$('.comments_form').submit(function(){

		

$.ajax({
	type: 'GET',
	url: 'LINKTOFILE.php',
	data: $(this).serialize(),
	dataType : 'json',
	beforeSend:function(){
		//$('#update span').html(befpreMsg);
	},
	success:function(data){
		
		
		
		if( data.error === false){
			//Success
			//$('#update span').html(successMsg);
			//results = true;
			$('#comment_field_'+data.id_feed).val(''); 
			$('#placing_'+data.id_feed).append(data.toprint);
			
			$('#comment_field_'+data.id_feed).animate({
					height: '20'
				}, 400, function() {
					// Animation complete.
			});
			
			
		}
		if(data.error === true){
			//$('#update span').html(errorMsg);
			//results = false;
			alert("Message has not been added");
		}
					
	},
	error:function(data){
		//$('#update span').html(errorMsg);
		//results = false;
	}
});	

		return false
	});	
	


	$('.add_network').live('click',function(){

		
$.ajax({
	type: 'GET',
	url: 'LINKTOFILE.php',
	data: { 'actions' : 'add_network' , 'new_id_user' : $(this).attr('title') },
	dataType : 'json',
	beforeSend:function(){
		//$('#update span').html(befpreMsg);
	},
	success:function(data){
		
		
		
		if( data.error === false){
			//Success
			//$('#update span').html(successMsg);
			//results = true;
			alert('Friend has been added to your network');
			
			
			
		}
		if(data.error === true){
			//$('#update span').html(errorMsg);
			//results = false;
			alert("Message has not been added");
		}
					
	},
	error:function(data){
		//$('#update span').html(errorMsg);
		//results = false;
	}
});	

		return false
	});	


	$('.remove_network').live('click',function(){
		
$.ajax({
	type: 'GET',
	url: 'LINKTOFILE.php',
	data: { 'actions' : 'remove_network' , 'new_id_user' : $(this).attr('title') },
	dataType : 'json',
	beforeSend:function(){
		//$('#update span').html(befpreMsg);
	},
	success:function(data){
				
		if( data.error === false){
		
			window.location.href = "";
			
		}
		if(data.error === true){

			alert("Message has not been added");
		}
					
	},
	error:function(data){

	}
});	

		return false
	});	


	
	$('#main_search').focus(function(){
			var value = $(this).val();
			var title = 'Search';
			
			if( value === title ){
				$(this).val('');
			}
			
	});
	
	
	
	$('#form_register').submit(function(){
									
									
$.ajax({
	type: 'GET',
	url: 'LINKTOFILE.php',
	data: $(this).serialize(),
	dataType : 'json',
	beforeSend:function(){
		//$('#update span').html(befpreMsg);
	},
	success:function(data){
		
		
		
		if( data.error === false){
			//Success
			//$('#update span').html(successMsg);
			//results = true;
			window.location.href = "";
			
		}
		if(data.error === true){
			//$('#update span').html(errorMsg);
			//results = false;
			alert("You have not been registered");
		}
					
	},
	error:function(data){
		//$('#update span').html(errorMsg);
		//results = false;
	}
});	

	return false;
									
	});  // END OF THE LOGIN 
	



	$('#profile_update').submit(function(){
									
									
$.ajax({
	type: 'GET',
	url: 'LINKTOFILE.php',
	data: $(this).serialize(),
	dataType : 'json',
	beforeSend:function(){
		//$('#update span').html(befpreMsg);
	},
	success:function(data){
		
		
		
		if( data.error === false){
			//Success
			//$('#update span').html(successMsg);
			//results = true;
			window.location.href = "";
			
		}
		if(data.error === true){
			//$('#update span').html(errorMsg);
			//results = false;
			alert("You have not been registered");
		}
					
	},
	error:function(data){
		//$('#update span').html(errorMsg);
		//results = false;
	}
});	

	return false;
									
	});  // END OF THE LOGIN    



	$('#profile_update_pwd').submit(function(){
									
									
$.ajax({
	type: 'GET',
	url: 'LINKTOFILE.php',
	data: $(this).serialize(),
	dataType : 'json',
	beforeSend:function(){
		//$('#update span').html(befpreMsg);
	},
	success:function(data){
		
		
		
		if( data.error === false){
			//Success
			$('#field_pwd').val('');
			$('#field_cpwd').val('');
			
		}
		if(data.error === true){
			//$('#update span').html(errorMsg);
			//results = false;
			alert("Password has not been changed");
		}
					
	},
	error:function(data){
		//$('#update span').html(errorMsg);
		//results = false;
	}
});	

	return false;
									
	});  // END OF THE LOGIN    profile_update_pwd


	$('#share_email').submit(function(){
									
$.ajax({
	type: 'GET',
	url: 'LINKTOFILE.php',
	data: $(this).serialize(),
	dataType : 'json',
	beforeSend:function(){
		//$('#update span').html(befpreMsg);
	},
	success:function(data){

		if( data.error === false){
	
			$('#share_email').fadeOut('fast');
			$('#share_email_response').fadeIn('fast');
			
		}
		if(data.error === true){
			//$('#update span').html(errorMsg);
			//results = false;
			alert("There was an error.  No email has been sent.");
		}
					
	},
	error:function(data){
		//$('#update span').html(errorMsg);
		//results = false;
	}
});	

	return false;
									
	});  // END OF THE LOGIN 

	
	
	$('.todo_status').live('click',function(){
		
		var value = $(this).is(':checked'); 
		if(value === true ){ value = 1; } else { value = 2; }

		var id_feed = $(this).val(); 
		
$.ajax({
	type: 'GET',
	url: 'LINKTOFILE.php',
	data: { 'actions' : 'update_todo_status' , 'id_feed' : id_feed , 'value' : value },
	dataType : 'json',
	beforeSend:function(){
		//$('#update span').html(befpreMsg);
	},
	success:function(data){

		if( data.error === false){
			if(value == 1){
				$('#text_'+id_feed).addClass('strikethrough');
			}
			if(value == 2){
				$('#text_'+id_feed).removeClass('strikethrough');
			}
			
		}
		if(data.error === true){
			//$('#update span').html(errorMsg);
			//results = false;
			alert("Update has not been completed");
		}
					
	},
	error:function(data){
		//$('#update span').html(errorMsg);
		//results = false;
	}
});	
									
	});  // END OF THE LOGIN 	
	
	
	$('.todo_status_module').live('click',function(){
		
		var value = $(this).is(':checked'); 
		if(value === true ){ value = 1; } else { value = 2; }

		var id_feed = $(this).val(); 
		var thing = $(this).parent('li');
		
$.ajax({
	type: 'GET',
	url: 'LINKTOFILE.php',
	data: { 'actions' : 'update_todo_status' , 'id_feed' : id_feed , 'value' : value },
	dataType : 'json',
	beforeSend:function(){
		//$('#update span').html(befpreMsg);
	},
	success:function(data){

		if( data.error === false){
			if(value == 1){
				$(thing).addClass('strikethrough');
			}
			if(value == 2){
				$(thing).removeClass('strikethrough');
			}
			
		}
		if(data.error === true){
			//$('#update span').html(errorMsg);
			//results = false;
			alert("Update has not been completed");
		}
					
	},
	error:function(data){
		//$('#update span').html(errorMsg);
		//results = false;
	}
});	
									
	});  // END OF THE LOGIN 	


	$('.remove_feed').live('click',function(){
		
		var id_feed = $(this).attr('id');
		
		var confirm_delete = confirm("Are you sure you want to delete this post?");
		if(!confirm_delete){ return false; }
		
		$.ajax({
			type: 'GET',
			url: 'LINKTOFILE.php',
			data: { 'actions' : 'remove_status' , 'id_feed' : id_feed },
			dataType : 'json',
			beforeSend:function(){
				//$('#update span').html(befpreMsg);
			},
			success:function(data){
		
				if( data.error === false){

					$('#story_box_'+id_feed).fadeOut('fast');
					
				}
				if(data.error === true){
					//$('#update span').html(errorMsg);
					//results = false;
					alert("Update has not been removed");
				}
							
			},
			error:function(data){
				//$('#update span').html(errorMsg);
				//results = false;
			}
		});	
		
		return false;
											
	});  // END OF THE LOGIN 		

	
});
