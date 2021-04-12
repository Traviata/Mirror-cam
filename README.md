
![image](https://user-images.githubusercontent.com/33504156/114340084-b722b380-9b91-11eb-88e7-33294185b26f.png)
![image](https://user-images.githubusercontent.com/33504156/114340157-e0434400-9b91-11eb-9ab3-460fdddc7f0b.png)
  인터넷 사용을 위한 권한 설정(자신의 ip가 화면 상단에 표시)
	Gstreamer 생성
	안드로이드 화면내의 객체와 자바 코드 변수의 연동
	서버를 열고 클라이언트 대기
	클라이언트 연결 시 클라이언트의 카메라 영상을 Gstreamer pipeline으로 수신
	pipeline으로 수신된 영상 데이터를 surfaceView에 표시
	버튼클릭 리스너 대기
	상/하, 좌/우, 촬영 버튼 입력 시 카메라 단말기로 제어신호 전달	
  
  ![image](https://user-images.githubusercontent.com/33504156/114340174-ed603300-9b91-11eb-92ef-abc393826c5d.png)
  인터넷 사용, 카메라, 데이터 저장을 위한 권한 설정
	서버의 Ip주소와 port번호 입력으로 서버 접속
	서버 접속 후 Gstreamer 생성
	Gstreamer의 pipeline을 이용한 영상 데이터 송신
	새로운 스레드를 생성하여 서버의 데이터 신호 수신을 위해 대기
	상/하, 좌/우 데이터를 수신하였을 때 아두이노로 해당 신호를 송신
	촬영 신호를 수신하였을 때 카메라 하드웨어를 제어하여 사진 촬영
  
  ![image](https://user-images.githubusercontent.com/33504156/114340186-f5b86e00-9b91-11eb-96c6-dfc2e6612a30.png)
  안드로이드와 시리얼 통신을 통해 제어 신호를 수신
	2개의 서보모터의 동작을 제어하여 상/하, 좌/우의 구도를 조정
