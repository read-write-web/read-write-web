<?php

	$disp = "";
	$disp .= "		<div class=\"listing_user\">\n"; 
	$disp .= "        	<div class=\"listing_user_left\">\n"; 
	$disp .= "            	<img src=\"{$user->picture}\" width=\"60\" height=\"60\" />\n"; 
	$disp .= "            </div>\n"; 
	$disp .= "            <div class=\"listing_user_right\">\n"; 
	$disp .= "            	<h3>{$user->username} </h3><h4>&nbsp; - {$user->name}</h4><br />\n"; 
	$disp .= "                {$user->about}<br />\n"; 
	$disp .= "                <span>{$user->skills}</span><br />\n"; 
	$disp .= "                \n"; 
	$disp .= "                <a href=\"\">Go to profile</a>\n"; 
	$disp .= "                {$friend_or_not}<br />\n"; 
	$disp .= "            </div>\n"; 
	$disp .= "            <div class=\"clear\"></div>\n"; 
	$disp .= "        </div>\n";	
	
	echo $disp;

?>