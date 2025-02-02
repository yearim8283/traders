/**
 * @author heera youn
 * @create date 2023-10-24 17:38:02
 * @modify date 2023-10-25 13:56:00
 * @desc [Mypage 찜목록 FE]
 */
/**
 * @author wheesunglee
 * @create date 2023-10-24 11:36:02
 * @modify date 2023-10-24 17:37:58
 */
import React, { useEffect, useState } from "react";
import ProductList from "../product/ProductList"; // ProductList 컴포넌트가 정의된 경로에 따라 수정
import TokenRefresher from "../util/TokenRefresher";
import { Col, Container, Row } from "react-bootstrap";

const MyLikes = (props) => {
  const [data, setData] = useState([]);
  useEffect(() => {
    TokenRefresher.get("http://localhost:8080/api/redis/myLikes")
      .then((res) => setData(res.data))
      .catch((error) => {
        if (error.response) {
          const errorResponse = error.response.data;
          console.log(errorResponse);
        }
      });
  }, []);
  console.log(data);
  if (props.tab === 1) {
    return (
        <>
        <Container style={{ maxWidth: "1040px", marginTop: "30px" }}>
            <Row className="justify-content-center" style={{ margin: "0px" }}>
                <Col md="12" style={{ margin: "0px" }}>
                    <Row>
                    {data &&
                        data.map((product, index) => {
                            return (
                                <Col md={3} key={index} style={{ marginBottom: "15px" }}>
                                <ProductList product={product} />
                                </Col>
                            );
                        })}
                </Row>
            </Col>
          </Row> 
        </Container>
      </>
    );
  } else {
    return null;
  }
};

export default MyLikes;