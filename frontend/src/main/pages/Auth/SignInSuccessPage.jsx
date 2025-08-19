import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import { useNavigate } from "react-router";

export default function SignInSuccessPage() {
  const storedReturn = sessionStorage.getItem("redirect");
  console.log(storedReturn);
  const navigation = useNavigate();
  if (storedReturn) {
    navigation(storedReturn);
  } else {
    navigation("/");
  }

  return (
    <BasicLayout>
      <h1>Redirecting...</h1>
    </BasicLayout>
  );
}
