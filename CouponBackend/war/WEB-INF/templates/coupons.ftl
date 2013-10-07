<!DOCTYPE HTML PUBLIC '-//W3C//DTD HTML 4.0 Transitional//EN'>
<HTML>
<HEAD><TITLE>Coupons</TITLE>
	<script src="http://ajax.googleapis.com/ajax/libs/jquery/2.0.0/jquery.min.js"></script>
	<script src="http://ajax.googleapis.com/ajax/libs/jqueryui/1.10.2/jquery-ui.min.js"></script>
	<link rel='stylesheet' type='text/css' href='stylesheets/coupons.css'>
	<link rel='stylesheet' type='text/css' href='stylesheets/jquery-ui.min.css'>
	<script type='text/javascript'>$(function() { $('.date').datepicker({dateFormat:'yy.mm.dd'}); });</script>
</HEAD>
<BODY>
	<#if !app.defined>
	<p>No app specified</p>
	<#else>
	<div id='content-container'>
		<div id='center-container'>
			<div id='title-container'>
				${app.name}
			</div>
			
			<div id='main-container'>
				<div id='middle-container'>
					<#if action == "generate">
					<div id='generated' class='box'>
						<h2>Generated coupon</h2>
						
						<#if coupon.exists>
						<p>Coupon code already in use. Instead generated ...</p>
						</#if>
						
						<h4>${coupon.code}</h4>
					</div>
					</#if>
					
					<#if action == "delete">
					<div id='delete' class='box'>
						<h2>Deleted coupon</h2>
						
						<#if !delete.specified>
						<p>Coupon code not specified.</p>
						<#else>
						
						<#if !delete.exists>
						<p>Coupon code ${delete.code} not found.</p>
						<#else>
						<p>Coupon code ${delete.code} deleted.</p>
						</#if>
						
						</#if>
						
					</div>
					</#if>
					
					<div id='saved' class='box'>
						<h2>Saved coupons</h2>
						
						<table border='1' style='border-collapse:collapse'>
							<tr>
								<th>Coupon</th>
								<th>One-time</th>
								<th>Redeemed</th>
								<th>Beginning</th>
								<th>End</th>
								<th>Created</th>
								<th>Users</th>
								<th>Date of redemption</th>
							</tr>
							
							<#list coupons as c>
							<tr>
								<td>${c.code}</td>
								<td>${c.one_time}</td>
								<td>${c.redeemed}</td>
								<td>${c.beginning}</td>
								<td>${c.end}</td>
								<td>${c.created}</td>
								<td>${c.users}</td>
								<td>${c.redemption_date}</td>
							</tr>
							</#list>
						</table>
						
					</div>
					
					<div id='timezone' class='box'>
						<h2>Timezone Info</h2>
						<p>TimeZone: ${time.zone_name}</p>
						<p>ID:  ${time.zone_id}</p>
						<p>Current time: ${time.current}</p>
					</div>
				</div>
			
				<div id='right-container'>
					<div id='create' class='box'>
						<h2>Generate a new coupon</h2>
						<form action='coupons'>
							<input type='hidden' name ='app' value='${app.name}'>
							<input type='hidden' name='action' value='generate'>
							<label>Coupon Code <input type='text' name='coupon'></label>
							<br/><br/>
							<label><input type='checkbox' name='onetime'> One-time</label>
								<h4>Beginning</h4><p>
							<div class='spaced'>
								<label>Date <input name='beginning_date' type='text' class='date'></label>
							</div>
							<label>Hour <input name='beginning_hour' type='text' size='2' maxlength='2'></label>
							<label>Minute <input name='beginning_minute' type='text' size='2' maxlength='2'></label>
							<br/>
								<h4>End</h4>
							<div class='spaced'>
								<label>Date <input name='end_date' type='text' class='date'></label>
							</div>
							<label>Hour <input name='end_hour' type='text' size='2' maxlength='2'></label>
							<label>Minute <input name='end_minute' type='text' size='2' maxlength='2'></label>
							<br/><br/>
								<input type='submit' value='Submit'>
						</form>
					</div>
					
					<div id='delete' class='box'>
						<h2>Delete a coupon</h2>
						<form action='coupons' >
							<input type='hidden' name ='app' value='${app.name}'>
							<input type='hidden' name='action' value='delete'>
							<label>Coupon Code <input type='text' name='coupon'></label>
							<br/><br/>
							<input type='submit' value='Submit'>
						</form>
					</div>
				
					<h2>
						<a href='coupons?app=${app.name}'>Refresh</a>
					</h2>
				</div>
			</div>
		</div>
	</div>
		
	</#if>
</BODY>
</HTML>