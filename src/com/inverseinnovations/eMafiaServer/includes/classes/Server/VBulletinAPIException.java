package com.inverseinnovations.eMafiaServer.includes.classes.Server;

public class VBulletinAPIException extends Exception{
	private static final long serialVersionUID = 1L;
	public VBulletinAPIException(){
		super("vBulletin API Error Returned - The forum is unable to complete your request");
	}
	public VBulletinAPIException(String message) { super(message); }
	public VBulletinAPIException(String message, Throwable cause) { super(message, cause); }
	public VBulletinAPIException(Throwable cause) { super(cause); }

	public class InvalidAPISignature extends VBulletinAPIException{
		private static final long serialVersionUID = 1L;
		public InvalidAPISignature(){
			super("InvalidAPISignature Exception - vBulletin API unable to process request due to the Hashed signature not consistent with the request");
		}
	}
	public class NoPermissionLoggedout extends VBulletinAPIException{
		private static final long serialVersionUID = 1L;
		public NoPermissionLoggedout(){
			super("NoPermissionLoggedout Exception - vBulletin API unable to process request without being logged in first");
		}
	}
	public class InvalidAccessToken extends VBulletinAPIException{
		private static final long serialVersionUID = 1L;
		public InvalidAccessToken(){
			super("InvalidAccessToken Exception - vBulletin API unable to process request due to incorrect token provided");
		}
	}
	public class MaxCharacterLengthReached extends VBulletinAPIException{
		private static final long serialVersionUID = 1L;
		public MaxCharacterLengthReached(){
			super("MaxCharacterLengthReached Exception - vBulletin API unable to process request due request being to many characters");
		}
	}
}
